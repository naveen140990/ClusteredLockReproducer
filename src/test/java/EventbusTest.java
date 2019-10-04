import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.unit.*;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.testng.annotations.Test;

import java.util.function.Consumer;


public class EventbusTest {

    static {
        System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
        System.setProperty("hazelcast.logging.type", "slf4j");
    }


    private static final Logger logger = LoggerFactory.getLogger(EventbusTest.class);

    private Vertx vertx;
    int numTestEvents=21;

    @Test
    public void test() {
        TestSuite testSuite = TestSuite.create("");
        testSuite.test("", testContest -> runTest(testContest));
        Completion completion = testSuite.run();
        completion.awaitSuccess();
    }

    private void runTest(TestContext testContext) {
        Async async = testContext.async();
        Future<Vertx> future = createVertx();
        future.setHandler(res -> {
            Vertx vertx = res.result();
            this.vertx=vertx;
            for (int i = 0; i < numTestEvents; i++) {
                getLock().setHandler( r-> {
                    if(r.succeeded()) {
                        vertx.eventBus().send("TEST", "TEST message");
                    }
                });
            }
        });
    }

    public Future<Boolean> getLock() {
        Future<Boolean> future = Future.future();

        String lock = "sample.lock";
        vertx.sharedData().getLock(lock, r -> {
            if (r.succeeded()) {
                logger.info("lock obtained for node {} and user {}");
                Lock asyncLock = r.result();
                asyncLock.release();
                future.complete(true);
            } else {
                r.cause().printStackTrace();
                future.fail("failed to obtain lock for self node");
            }
        });
        return future;
    }


    private Future<Vertx> createVertx() {
        Future<Vertx> future = Future.future();
        Consumer<Vertx> runner = vertx -> {
            try {
                vertx.deployVerticle(() -> new Tester(), new DeploymentOptions().setInstances(1), res -> {
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
        ClusterManager clusterManager = new HazelcastClusterManager();
        if (clusterManager != null) {
            Vertx.clusteredVertx(new VertxOptions().setClusterManager(clusterManager), res -> {
                if (res.succeeded()) {
                    Vertx vertx = res.result();
                    runner.accept(vertx);
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

    public static class Tester extends AbstractVerticle {
        @Override
        public void start() {
                vertx.eventBus().localConsumer("TEST", testHandler());
        }

        private Handler<Message<String>> testHandler() {
            return message -> {
                logger.info(message.body());
            };
        }

    }


}
