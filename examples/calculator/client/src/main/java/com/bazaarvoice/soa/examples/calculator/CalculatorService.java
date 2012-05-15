package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.Service;

public interface CalculatorService extends Service {
    int add(int a, int b);
    int sub(int a, int b);
    int mul(int a, int b);
    int div(int a, int b);
}
