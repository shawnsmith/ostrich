package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.HostDiscovery;
import com.bazaarvoice.soa.ServiceCallback;
import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.ServiceException;
import com.bazaarvoice.soa.ServicePool;
import com.bazaarvoice.soa.pool.ServicePoolBuilder;
import com.bazaarvoice.soa.retry.RetryNTimes;

import java.util.Collections;

public class CalculatorUser {
    private final ServicePool<CalculatorService> _calculatorPool;

    public CalculatorUser(ServicePool<CalculatorService> calculatorPool) {
        _calculatorPool = calculatorPool;
    }

    public void use() {
        int sum = _calculatorPool.execute(new RetryNTimes(1), new ServiceCallback<CalculatorService, Integer>() {
            @Override
            public Integer call(CalculatorService service) throws ServiceException {
                return service.add(1, 2);
            }
        });
        System.out.println("sum: " + sum);
    }

    public static void main(String[] args) {
        HostDiscovery localDiscovery = new HostDiscovery() {
            ServiceEndpoint LOCAL = new ServiceEndpoint("calculator", "localhost", 8081);

            @Override
            public Iterable<ServiceEndpoint> getHosts() {
                return Collections.singleton(LOCAL);
            }
        };

        ServicePool<CalculatorService> pool = new ServicePoolBuilder<CalculatorService>()
                .withHostDiscovery(localDiscovery)
                .withServiceFactory(new CalculatorServiceFactory())
                .build();

        CalculatorUser user = new CalculatorUser(pool);
        user.use();
    }
}
