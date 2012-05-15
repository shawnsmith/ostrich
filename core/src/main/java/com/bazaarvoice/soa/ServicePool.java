package com.bazaarvoice.soa;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A <code>ServicePool</code> keeps track of <code>Service</code> instances for a particular service.  Internally it
 * understands how requests should be load balanced across instances and takes a service owner's
 * <code>LoadBalanceAlgorithm</code> into account when deciding which instance to use.  In addition the
 * <code>ServicePool</code> is also able to monitor the health of a service instance if the service owner provides a
 * health check implementation.  It is able to use the health check information as a guide in selecting service
 * instances in order to avoid providing an unhealthy service instance to a user.
 * <p/>
 * The <code>ServicePool</code> provides an automatically managed resource pool model to consumers.  A consumer
 * provides a callback to the <code>ServicePool</code>  to execute a piece of code against a service instance.  The
 * <code>ServicePool</code> will then select a suitable service instance to use and then invoke the user's callback
 * with a handle to the instance.  At that point the user can interact with the remote instance however it wants,
 * calling any APIs necessary.  When the callback returns, the connection with the remote service is cleaned up.  If
 * during the execution of the callback a service related error occurs, the service instance will be marked as
 * unhealthy, and the operation retried, as allowed by the <code>RetryPolicy</code> the user specifies.
 * <p/>
 * For example, assume that we have a mythical <code>CalculatorService</code> with <code>add</code>, <code>sub</code>,
 * etc. methods on it.  Usage of a <code>ServicePool</code> for that service might look like:
 * <pre>
 * int sum = calculatorPool.execute(new RetryNTimes(3), new ServiceCallback<CalculatorService, Integer>() {
 *     public Integer call(CalculatorService calculator) {
 *         return calculator.add(1, calculator.sub(3, 2));
 *     }
 * });
 * </pre>
 *
 * @param <S> The <code>Service</code> subclass that this pool keeps track of instances for.
 */
public interface ServicePool<S extends Service> {
    /**
     * Execute a request synchronously against one of the remote services in this <code>ServicePool</code>.
     *
     * @param retryPolicy The retry policy for the operation.
     * @param callback The user provided callback to invoke with a service instance.
     * @param <R> The return type for the call.
     */
    <R> R execute(RetryPolicy retryPolicy, ServiceCallback<S, R> callback);

    /**
     * Execute a request asynchronously against one of the remote services in this <code>ServicePool</code>.  A
     * <code>ListenableFuture</code> is returned that will eventually contain the result of the operation.
     *
     * @param retryPolicy The retry policy for the operation.
     * @param callback The user provided callback to invoke with a service instance.
     * @param <R> The return type for the call.
     */
    <R> ListenableFuture<R> executeAsync(RetryPolicy retryPolicy, ServiceCallback<S, R> callback);
}