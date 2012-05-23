package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.ServiceCallback;
import com.bazaarvoice.soa.ServiceException;
import com.bazaarvoice.soa.ServicePool;
import com.bazaarvoice.soa.discovery.ZooKeeperHostDiscovery;
import com.bazaarvoice.soa.pool.ServicePoolBuilder;
import com.bazaarvoice.soa.retry.RetryNTimes;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfigurationBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class CalculatorUser {
    private final ServicePool<CalculatorService> _calculatorPool;

    public CalculatorUser(ServicePool<CalculatorService> calculatorPool) {
        _calculatorPool = calculatorPool;
    }

    public void use() throws InterruptedException {
        int i = 0;

        //noinspection InfiniteLoopStatement
        while (true) {
            int sum = _calculatorPool.execute(new RetryNTimes(3, 100, TimeUnit.MILLISECONDS),
                    new ServiceCallback<CalculatorService, Integer>() {
                        @Override
                        public Integer call(CalculatorService service) throws ServiceException {
                            return service.add(1, 2);
                        }
                    });
            System.out.println("i: " + i + ", sum: " + sum);
            Thread.sleep(100);
            i++;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ZooKeeperConfiguration config = new ZooKeeperConfigurationBuilder()
                .withConnectString("localhost:2181")
                .withRetryNTimes(3, 100)
                .build();

        ThreadFactory daemonThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .build();

        ServicePool<CalculatorService> pool = new ServicePoolBuilder<CalculatorService>()
                .withHostDiscovery(new ZooKeeperHostDiscovery(config, "calculator"))
                .withServiceFactory(new CalculatorServiceFactory())
                .withHealthCheckExecutor(Executors.newScheduledThreadPool(1, daemonThreadFactory))
                .build();

        CalculatorUser user = new CalculatorUser(pool);
        user.use();
    }
}
