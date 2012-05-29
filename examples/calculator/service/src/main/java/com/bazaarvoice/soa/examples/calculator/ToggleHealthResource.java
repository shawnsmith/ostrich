package com.bazaarvoice.soa.examples.calculator;

import com.yammer.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class ToggleHealthResource {
    @GET
    @Timed
    @Path("/{state}")
    public boolean add(@PathParam("state") boolean newState) {
        CalculatorService.IS_HEALTHY = newState;
        return CalculatorService.IS_HEALTHY;
    }
}
