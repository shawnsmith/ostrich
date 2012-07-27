package com.bazaarvoice.soa.pool;

import com.bazaarvoice.soa.RetryPolicy;
import com.bazaarvoice.soa.ServiceCallback;
import com.bazaarvoice.soa.exceptions.ServiceException;
import com.google.common.base.Throwables;
import com.google.common.reflect.AbstractInvocationHandler;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ServicePoolProxy {
    public static <S> S newProxy(final Class<S> serviceType, final RetryPolicy retryPolicy, final ServicePool<S> pool,
                                 final boolean shutdownPoolOnClose) {
        checkNotNull(serviceType);
        checkState(serviceType.isInterface(), "Proxy functionality is only available for interface service types.");
        checkNotNull(retryPolicy);
        checkNotNull(pool);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?>[] interfaces = shutdownPoolOnClose
                ? new Class<?>[] { serviceType, Closeable.class }
                : new Class<?>[] { serviceType };

        Object proxy = Proxy.newProxyInstance(loader, interfaces, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(Object proxy, final Method method, final Object[] args) throws Throwable {
                // Special case for close() allows closing the entire pool by calling close() on the proxy.
                if (shutdownPoolOnClose && args.length == 0 && method.getName().equals("close")) {
                    pool.close();
                    return null;
                }

                // Delegate the method through to a service provider in the pool.
                return pool.execute(retryPolicy, new ServiceCallback<S, Object>() {
                    @Override
                    public Object call(S service) throws ServiceException {
                        try {
                            return method.invoke(service, args);
                        } catch (IllegalAccessException e) {
                            throw Throwables.propagate(e);
                        } catch (InvocationTargetException e) {
                            throw Throwables.propagate(e.getTargetException());
                        }
                    }
                });
            }

            @Override
            public String toString() {
                return "ServicePoolProxy[" + serviceType.getName() + "]";
            }
        });
        return serviceType.cast(proxy);
    }
}
