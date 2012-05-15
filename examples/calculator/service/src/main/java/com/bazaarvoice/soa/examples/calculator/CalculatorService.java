package com.bazaarvoice.soa.examples.calculator;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;

public class CalculatorService extends Service<Configuration> {
    public CalculatorService() {
        super("calculator");
    }

    @Override
    protected void initialize(Configuration config, Environment env) throws Exception {
        env.addResource(CalculatorResource.class);
    }

    public static void main(String[] args) throws Exception {
        new CalculatorService().run(args);
    }
}
