package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.ServiceEndPoint;
import com.bazaarvoice.soa.ServiceEndPointBuilder;
import com.bazaarvoice.soa.ServiceRegistry;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConnection;
import com.google.common.collect.ImmutableMap;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;

import java.net.InetAddress;
import java.net.URL;

public class CalculatorService extends Service<CalculatorConfiguration> {
    public static boolean IS_HEALTHY = true;

    public CalculatorService() {
        super("calculator");
    }

    @Override
    protected void initialize(CalculatorConfiguration config, Environment env) throws Exception {
        env.addResource(CalculatorResource.class);
        env.addResource(ToggleHealthResource.class);
        env.addHealthCheck(new CalculatorHealthCheck());

        String ip = InetAddress.getLocalHost().getHostAddress();
        int port = config.getHttpConfiguration().getPort();
        int adminPort = config.getHttpConfiguration().getAdminPort();

        // The client reads the URLs out of the payload to figure out how to connect to this server.
        String payload = getJson().writeValueAsString(ImmutableMap.builder()
                .put("url", new URL("http", ip, port, "/" + getName()))
                .put("adminUrl", new URL("http", ip, adminPort, ""))
                .build());
        final ServiceEndPoint endpoint = new ServiceEndPointBuilder()
                .withServiceName(getName())
                .withHostname(ip)
                .withPort(port)
                .withPayload(payload)
                .build();

        // Once everything has initialized successfully, register services with ZooKeeper where clients can find them.
        ZooKeeperConnection connection = config.getZooKeeperConfiguration().connect();
        final ServiceRegistry registry = new ZooKeeperServiceRegistry(connection);
        env.manage(new Managed() {
            @Override
            public void start() throws Exception {
                registry.register(endpoint);
            }

            @Override
            public void stop() throws Exception {
                registry.unregister(endpoint);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        new CalculatorService().run(args);
    }
}
