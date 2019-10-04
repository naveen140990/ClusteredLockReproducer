import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.cluster.infinispan.InfinispanClusterManager;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.report.ReportOptions;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


public class EventbusTest {

    static {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.WARN);
        System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
        System.setProperty("hazelcast.logging.type", "slf4j");
    }
    private static final Logger logger = LoggerFactory.getLogger(EventbusTest.class);
    private static final String MESSAGE = ":)";
    private static final String DURATION = "10";  // seconds

    private static final DeliveryOptions DEFAULT_DELIVERY = new DeliveryOptions();
    //private static final DeliveryOptions LO_ONLY_DELIVERY = new DeliveryOptions().setLocalOnly(true);
    enum ClusterMethod { None, Hazelcast, Infinispan, Zookeeper }
    enum DeliveryMethod { Send_Send }



    private AsyncMap<String, GlobalSession> globalMap;
    // TODO:rename variables properly
    private AsyncMap<String, Set<String>> clusterWiseUserTracker;
    private Set<String> localUsers = new HashSet<>();
    private Vertx vertx;

    public static class TestVariables {
        int numInstances;
        int numTestEvents;
        ClusterMethod clusterMethod;
        boolean useLocalConsumer;
        DeliveryOptions deliveryOptions;
        DeliveryMethod deliveryMethod;
        TestVariables(int numInstances, int numTestEvents, ClusterMethod clusterMethod, boolean useLocalConsumer, DeliveryOptions deliveryOptions, DeliveryMethod deliveryMethod) {
            this.numInstances = numInstances;
            this.numTestEvents = numTestEvents;
            this.clusterMethod = clusterMethod;
            this.useLocalConsumer = useLocalConsumer;
            this.deliveryOptions = deliveryOptions;
            this.deliveryMethod = deliveryMethod;
        }
        @Override
        public String toString() {
            return "TestVariables: " +
                    "numInstances=" + numInstances +
                    ", numTestEvents=" + numTestEvents +
                    ", clusterMethod=" + clusterMethod +
                    ", useLocalConsumer=" + useLocalConsumer +
                    ", deliveryOptions=" + (deliveryOptions == DEFAULT_DELIVERY ? "default" : "localOnly") +
                    ", deliveryMethod=" + deliveryMethod;
        }
    }
    private TestVariables V(int numInstances, int numTestEvents, ClusterMethod clusterMethod, boolean useLocalConsumer, DeliveryOptions deliveryOptions, DeliveryMethod deliveryMethod) {
        return new TestVariables(numInstances, numTestEvents, clusterMethod, useLocalConsumer, deliveryOptions, deliveryMethod);
    }

    @DataProvider(name = "dataProvider")
    public Object[][] dataProvider() {
        return new Object[][] {

                { V(2, 21, ClusterMethod.Hazelcast, true, DEFAULT_DELIVERY, DeliveryMethod.Send_Send) },


        };
    }

    @Test(invocationCount = 1, dataProvider = "dataProvider")
    public void test(TestVariables testVariables) {
        long duration = Long.parseLong(System.getProperty("duration", DURATION));
        TestSuite testSuite = TestSuite.create("");
        testSuite.test("", testContest -> runTest(testContest, duration, testVariables));
        Completion completion = testSuite.run(new TestOptions().addReporter(new ReportOptions().setTo("console")).setTimeout(10 * 60 * 1000L));
        completion.awaitSuccess();
    }

    private void runTest(TestContext testContext, long duration, TestVariables testVariables) {
        Async async = testContext.async();
        Future<Vertx> future = createVertx(testVariables);
        future.setHandler(res -> {
            Vertx vertx = res.result();
            this.vertx=vertx;


            logger.info("{}", testVariables);
            for (int i = 0; i < testVariables.numTestEvents; i++) {
                User user = new User(""+i,i+"email@gmail.com",i+"email");
                GlobalSession session = new GlobalSession(Long.valueOf(user.getId()),user );
                addGlobalSession(session).subscribe(onNext -> {
                    vertx.eventBus().send("TEST", duration, testVariables.deliveryOptions);
                });

            }
        });
    }

    private Future<Vertx> createVertx(TestVariables testVariables) {
        Future<Vertx> future = Future.future();
        Consumer<Vertx> runner = vertx -> {
            try {
                vertx.deployVerticle(() -> new Tester(testVariables), new DeploymentOptions().setInstances(testVariables.numInstances), res -> {
                    if (res.succeeded()) {
                        logger.info("Vertx started successfully");
                        future.complete(vertx);
                    } else {
                        logger.warn("Vertx failed to start", res.cause());
                        future.fail(res.cause());
                    }
                });
            } catch (Throwable t) {
                logger.warn("Vertx failed to start", t);
                future.fail(t);
            }
        };
        ClusterManager clusterManager;
        ObservableFuture<AsyncMap<String, GlobalSession>> mapObserbable = RxHelper.observableFuture();
        switch (testVariables.clusterMethod) {
            case Hazelcast: clusterManager = new HazelcastClusterManager(); break;
            case Infinispan: clusterManager = new InfinispanClusterManager(); break;
            case Zookeeper: clusterManager = new ZookeeperClusterManager(); break;
            default: clusterManager = null;
        }
        if (clusterManager != null) {
            Vertx.clusteredVertx(new VertxOptions().setClusterManager(clusterManager), res -> {
                if (res.succeeded()) {
                    Vertx vertx = res.result();
                    runner.accept(vertx);

                    vertx.sharedData().getClusterWideMap(Constants.GLOBAL_USER_SESSION_MAP_NAME,
                            mapObserbable.toHandler());
                    mapObserbable.subscribe(map -> {
                        globalMap = map;
                    }, onError -> {
                        // TODO:propagate this error
                        onError.printStackTrace();
                    });

                    ObservableFuture<AsyncMap<String, Set<String>>> globalIdMultimap = RxHelper.observableFuture();
                    vertx.sharedData().getClusterWideMap(Constants.CLUSTER_WISE_USER_MAP, globalIdMultimap.toHandler());
                    globalIdMultimap.subscribe(multimap -> {
                        clusterWiseUserTracker = multimap;
                        clusterWiseUserTracker.put(getNodeId(), localUsers, result -> {
                            if (result.succeeded()) {
                                logger.info("success in creating local user map on cluster for node{}",getNodeId());
                            } else {
                                result.cause().printStackTrace();
                            }
                        });
                    }, onError -> {
                        onError.printStackTrace();
                    });

                } else {
                    logger.warn("Vertx failed to start", res.cause());
                    future.fail(res.cause());
                }
            });
        } else {
            Vertx vertx = Vertx.vertx();
            runner.accept(vertx);
        }
        return future;
    }

    public ObservableFuture<GlobalSession> addGlobalSession(GlobalSession globalSession) {
        ObservableFuture<GlobalSession> mapResult = RxHelper.observableFuture();
        String nodeId = getNodeId();
        logger.debug("trying to acquire lock for node {} and user {}",nodeId,globalSession);
        String lock = "com.mettl.chat.router.lock.clusterWiseUserTracker" + nodeId;
        vertx.sharedData().getLock(lock, r -> {
            if (r.succeeded()) {
                logger.info("lock obtained for node {} and user {}",nodeId,globalSession);
                final io.vertx.core.shareddata.Lock asyncLock = r.result();
                localUsers.add(globalSession.getUser().getId());
                clusterWiseUserTracker.put(getNodeId(), localUsers, e->{
                    logger.info("cluster update success for user {} on node {} releasing lock",globalSession.getUser().getId(),getNodeId());
                    asyncLock.release();
                });
                globalMap.putIfAbsent(globalSession.getUser().getId(), globalSession, mapResult.toHandler());
            } else {
                r.cause().printStackTrace();
                logger.error("error in obtaining lock for node {} and user {}",getNodeId(),globalSession.getUser().getId());
                mapResult.toHandler().handle(Future.failedFuture("failed to obtain lock for self node"));
            }
        });
        return mapResult;
    }

    private String getNodeId() {
        ClusterManager clusterManager = ((VertxInternal) vertx).getClusterManager();
        return clusterManager.getNodeID();
    }

    public static class Tester extends AbstractVerticle {
        private TestVariables testVariables;
        Tester(TestVariables testVariables) {
            this.testVariables = testVariables;
        }
        @Override
        public void start() {
            if (testVariables.useLocalConsumer) {
                vertx.eventBus().localConsumer("TEST", testHandler());
            }
        }

        private Handler<Message<Object>> testHandler() {
            return message -> {


                System.out.println(message.toString());
            };
        }




    }




}
