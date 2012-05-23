package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.ServiceRegistry;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfigurationBuilder;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;

import java.net.InetAddress;

public class CalculatorService extends Service<Configuration> {
    public CalculatorService() {
        super("calculator");
    }

    @Override
    protected void initialize(Configuration config, Environment env) throws Exception {
        env.addResource(CalculatorResource.class);

        String hostname = InetAddress.getLocalHost().getHostName();
        int port = config.getHttpConfiguration().getPort();
        ServiceEndpoint endpoint = new ServiceEndpoint(getName(), hostname, port);

        // Register with ZooKeeper
        ZooKeeperConfiguration zooKeeperConfig = new ZooKeeperConfigurationBuilder()
                .withConnectString("localhost:2181")
                .withRetryNTimes(3, 100)
                .build();
        ServiceRegistry registry = new ZooKeeperServiceRegistry(zooKeeperConfig);
        registry.register(endpoint);
    }

    public static void main(String[] args) throws Exception {
        new CalculatorService().run(args);
    }
}
