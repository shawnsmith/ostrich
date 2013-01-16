package com.bazaarvoice.soa;

import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.zookeeper.ZooKeeperConfiguration;
import com.bazaarvoice.zookeeper.ZooKeeperConnection;

public class StressTest {
    public static void main(String[] args) throws InterruptedException {
        ZooKeeperConnection connection = new ZooKeeperConfiguration().connect();
        ZooKeeperServiceRegistry registry = new ZooKeeperServiceRegistry(connection);

        ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withId("1")
                .withServiceName("stress-test")
                .withPayload("payload")
                .build();

        registry.register(endpoint);

        Object o = new Object();
        synchronized (o) {
            o.wait();
        }
    }
}