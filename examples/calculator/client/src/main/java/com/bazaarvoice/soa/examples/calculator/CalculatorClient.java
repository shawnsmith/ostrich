package com.bazaarvoice.soa.examples.calculator;

import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.ServiceException;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class CalculatorClient implements CalculatorService {
    private final String _baseUrl;

    public CalculatorClient(ServiceEndpoint endpoint) {
        _baseUrl = "http://" + endpoint.getServiceAddress() + "/calculator";
    }

    @Override
    public int add(int a, int b) {
        return Integer.parseInt(GET("/add/" + a + "/" + b));
    }

    @Override
    public int sub(int a, int b) {
        return Integer.parseInt(GET("/sub/" + a + "/" + b));
    }

    @Override
    public int mul(int a, int b) {
        return Integer.parseInt(GET("/mul/" + a + "/" + b));
    }

    @Override
    public int div(int a, int b) {
        return Integer.parseInt(GET("/div/" + a + "/" + b));
    }

    private String GET(String resource) {
        URL url;
        try {
            url = new URL(_baseUrl + resource);
        } catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        }

        InputStream stream = null;
        try {
            stream = url.openStream();
            byte[] bytes = ByteStreams.toByteArray(stream);
            return new String(bytes, Charsets.UTF_8);
        } catch (IOException e) {
            // Was an exception processing the request -- retrying could solve it.
            throw new ServiceException(e);
        } finally {
            Closeables.closeQuietly(stream);
        }
    }
}
