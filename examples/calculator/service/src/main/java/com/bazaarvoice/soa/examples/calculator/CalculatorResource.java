package com.bazaarvoice.soa.examples.calculator;

import com.yammer.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

@Path("/calculator")
@Produces(MediaType.APPLICATION_JSON)
public class CalculatorResource {
    @GET
    @Timed
    @Path("/add/{arg1}/{arg2}")
    public int add(@PathParam("arg1") int arg1, @PathParam("arg2") int arg2) {
        if (!CalculatorService.IS_HEALTHY) throw new WebApplicationException(500);
        return arg1 + arg2;
    }

    @GET
    @Timed
    @Path("/sub/{arg1}/{arg2}")
    public int sub(@PathParam("arg1") int arg1, @PathParam("arg2") int arg2) {
        if (!CalculatorService.IS_HEALTHY) throw new WebApplicationException(500);
        return arg1 - arg2;
    }

    @GET
    @Timed
    @Path("/mul/{arg1}/{arg2}")
    public int mul(@PathParam("arg1") int arg1, @PathParam("arg2") int arg2) {
        if (!CalculatorService.IS_HEALTHY) throw new WebApplicationException(500);
        return arg1 * arg2;
    }

    @GET
    @Timed
    @Path("/div/{arg1}/{arg2}")
    public int div(@PathParam("arg1") int arg1, @PathParam("arg2") int arg2) {
        if (!CalculatorService.IS_HEALTHY) throw new WebApplicationException(500);
        return arg1 / arg2;
    }
}