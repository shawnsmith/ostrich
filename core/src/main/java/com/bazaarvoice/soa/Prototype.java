package com.bazaarvoice.soa;

@SuppressWarnings("UnusedDeclaration")
public class Prototype {
    ////////////////////////////////////////////////////////////
    // Common code written by the infrastructure team
    ////////////////////////////////////////////////////////////

    // Service registration API (for service providers to use)

    /*
    // Retry logic
    private static interface RetryPolicy {}
    private static final class ExponentialBackoff implements RetryPolicy {}
    private static final class RetryNTimes implements RetryPolicy {}
    private static final class RetryUntilElapsed implements RetryPolicy {}

    // Load balancing
    private static interface LoadBalanceAlgorithm {}
    private static final class LeastLoaded implements LoadBalanceAlgorithm {}
    private static final class LeastNumConnections implements LoadBalanceAlgorithm {}
    private static final class LeastLatency implements LoadBalanceAlgorithm {}

    // TODO: Separate pool of instances from retry policy so the pool of instances can be reused
    // Client consumption interfaces
    public class ServicePoolBuilder<S extends Service> {
        ServicePoolBuilder<S> withCuratorFramework(CuratorFramework framework) { return this; }
        ServicePoolBuilder<S> withServiceFactory(ServiceFactory<S> factory) { return this; }
        ServicePool<S> build() { return null; }
    }

    public interface ServicePool<S extends Service> {
        <R> R execute(RetryPolicy retryPolicy, ServiceCallback<S, R> callback);
        <R> ListenableFuture<R> executeInBackground(RetryPolicy retryPolicy, ServiceCallback<S, R> callback);
    }
    public interface ServiceCallback<S extends Service, R> {
        R call(S service);
    }

    public interface Service {
        boolean isHealthy();
    }
    public interface ServiceFactory<S extends Service> {
        // TODO: getServicePath and getLoadBalanceAlgorithm don't feel right here.
        String getServicePath();
        LoadBalanceAlgorithm getLoadBalanceAlgorithm();
        S create(ServiceInstance instance);
    }

    // In ZNode for logical service (e.g. /services/calculator/v1):
    //   NOTHING!!!
    //
    // In ephemeral ZNode for physical service (e.g. /services/calculator/v1/hostname:port):
    // {
    //   "address": "faws-prod-c1-calc1.aws.bazaarvoice.com",
    //   "port": 1234,
    //   "registrationTimeUTC": 1335460830791,
    //   "currentLoad": 2,                        <-- can be used by the load balance algorithm to aid decision making
    //   "payload": null,                         <-- will be passed to provider code when instantiating a new instance
    // }


    ////////////////////////////////////////////////////////////
    // Code that service providers need to write
    ////////////////////////////////////////////////////////////
    public static interface CalculatorService extends Service {
        int add(int a, int b);
        int sub(int a, int b);
        int mul(int a, int b);
        int div(int a, int b);
    }

    public static class HttpCalculatorService implements CalculatorService {
        private final URLConnection urlConnection = null;
        public HttpCalculatorService(ServiceInstance instance) {}
        public int add(int a, int b) { return get("/calculator/add/" + a + "/" + b); }
        public int sub(int a, int b) { return get("/calculator/sub/" + a + "/" + b); }
        public int mul(int a, int b) { return get("/calculator/mul/" + a + "/" + b); }
        public int div(int a, int b) { return get("/calculator/div/" + a + "/" + b); }
        public boolean isHealthy() { return true; }
        private static int get(String resource) { return 0; }
    }

    // TODO: Maybe this should be renamed?  It seems like it's mixing a couple of concerns.
    public static class CalculatorServiceFactory implements ServiceFactory<CalculatorService> {
        public String getServicePath() { return "calculator/v1"; }
        public LoadBalanceAlgorithm getLoadBalanceAlgorithm() { return new LeastLoaded(); }
        public CalculatorService create(ServiceInstance instance) { return new HttpCalculatorService(instance); }
    }

    // TODO: Provide Guice module and Spring XML


    //
    // Q: How does the client side figure out that when someone builds a ServiceProvider for CalculatorService that
    // they should instantiate an instance of HttpCalculatorService?
    //
    // IDEAS:
    //   - Put it in ZooKeeper.  What about version skew?  There's only one field in ZooKeeper for where this could go,
    //     but what happens when someone needs to update things?  What happens when the field gets updated but someone
    //     using the service doesn't update their jar?
    //
    //   - Put it in the jar that the team who writes the provider ships?  It could exist of a META-INF/services entry.
    //     This would be nice because it would guarantee that the jar that shipped was always functional, but one con to
    //     this approach is that it's different and not really like anything else we have.  It also uses a paradigm that
    //     isn't really repeatable in other languages.
    //
    //   - Include it in the jar and have a convention that non-backwards compatible changes to the client side or
    //     server side must accompany a bump in service version?
    //
    //
    // Q: How does the HttpCalculatorService receive the instance information (address, port, payload)?
    //
    // IDEAS:
    //   - Use reflection to call a constructor with a known/required signature?  Or maybe to call an interface required
    //     initialize method.
    //
    // A: Use a Factory interface with a create method that takes a HostAndPort, etc. as parameters.
    //
    //
    // Q: How do we figure out exactly which node in ZooKeeper to look at to identify metadata about the service.  All
    // we currently have to go on is the service class name.  But version number is a critical portion of the ZooKeeper
    // path.
    //
    // IDEAS:
    //   - Bake the version number into the class name.
    //   - Register a name to implementation mapping
    //


    ////////////////////////////////////////////////////////////
    // Code that service consumers need to write
    ////////////////////////////////////////////////////////////
    {
        CuratorFramework curator = null;

        // Ideally this is done once and then injected wherever it's needed
        ServicePool<CalculatorService> calculatorPool = new ServicePoolBuilder<CalculatorService>()
                .withCuratorFramework(curator)
                .withServiceFactory(new CalculatorServiceFactory())
                .build();

        int sum = calculatorPool.execute(new ExponentialBackoff(), new ServiceCallback<CalculatorService, Integer>() {
            public Integer call(CalculatorService calculator) {
                return calculator.add(1, 2);
            }
        });

        ListenableFuture<Integer> difference = calculatorPool.executeInBackground(new ExponentialBackoff(),
                new ServiceCallback<CalculatorService, Integer>() {
                    public Integer call(CalculatorService calculator) {
                        return calculator.sub(1, 2);
                    }
                });
        //int diff = difference.get();
    }
    */


//    ////////////////////////////////////////////////////////////
//    // Common code offered up by the infrastructure team
//    ////////////////////////////////////////////////////////////
//    private static interface Service {}
//    private static interface Client<S extends Service> {
//        Class<S> getServiceClass();
//    }
//    private static interface RetryPolicy {}
//    private static interface LoadBalanceAlgorithm {}
//
//    private static abstract class Services {
//        public static RetryPolicy getRetryPolicy(Class<? extends Service> serviceClass) { return null; }
//        public static LoadBalanceAlgorithm getLoadBalanceAlgorithm(Class<? extends Service> serviceClass) { return null; }
//
//        public static <C extends Client, R> R executeWith(Class<C> clientClass, ServiceCallback<C, R> callback) { return null; }
//        public static <C extends Client> void executeWith(Class<C> clientClass, ServiceCallbackNoResult<C> callback) { }
//    }
//
//    private static interface ServiceCallback<C extends Client, R> {
//        R call(C client);
//    }
//    private static interface ServiceCallbackNoResult<C extends Client> {
//        void call(C client);
//    }
//
//    private static interface ServiceRegistry {
//        void register(Service service);
//        void unregister(Service service);
//    }
//
//
//    // In ZNode for logical service (e.g. /services/calculator/v1):
//    // {
//    //   "clientFactoryClass"       : "com.bazaarvoice.calculator.CalculatorClientFactory",  // TODO: This shouldn't be java specific
//    //   "loadBalanceAlgorithmClass": "com.bazaarvoice.soa.RandomLoadBalanceAlgorithm",      // TODO: This shouldn't be java specific
//    // }
//    //
//    // In ephemeral ZNode for physical service (e.g. /services/calculator/v1/hostname:port):
//    // {
//    //   "address": "aws-prod-c1-calc1.aws.bazaarvoice.com",
//    //   "port": 1234,
//    //   "registrationTimeUTC": 1335460830791,
//    //   "currentLoad": 2,                        <-- can be used by the load balance algorithm to aid decision making
//    //   "payload": null,                         <-- will be passed to provider code when instantiating a new client
//    // }
//
//
//
//    // TODO: Where do ClientFactories fit into this model...
//    private static interface ClientFactory<T> {
//        T create(Properties propsFromZookeeper);
//        boolean isHealthy(T tg);
//    }
//
//    //
//    // To help facilitate client creation we may offer some client factory instances that support things like
//    // dynamic client generation for Jersey services, thrift services, etc.
//    //
//
//
//
//    ////////////////////////////////////////////////////////////
//    // Code that service providers need to write
//    ////////////////////////////////////////////////////////////
//    private static class Provider {
//        public static interface CalculatorService extends Service {
//            int add(int a, int b);
//            int sub(int a, int b);
//            int mul(int a, int b);
//            int div(int a, int b);
//        }
//        private static final class CalculatorServiceImpl implements CalculatorService {
//            public int add(int a, int b) { return a+b; }
//            public int sub(int a, int b) { return a-b; }
//            public int mul(int a, int b) { return a*b; }
//            public int div(int a, int b) { return a/b; }
//        }
//
//        public static interface CalculatorClient extends Client<CalculatorService> {
//            int add(int a, int b);
//            int sub(int a, int b);
//            int mul(int a, int b);
//        }
//
//        // This implementation encapsulates what the communication protocol and message format are.
//        // Each team that implements a client is free to choose whatever they'd like here.
//        public static final class CalculatorClientImpl implements CalculatorClient {
//            public Class<CalculatorService> getServiceClass() { return CalculatorService.class; }
//
//            public int add(int a, int b) {
//                //HttpURLConnection cxn = newRequestBuilder("GET").path("add", a, b).execute();
//                //return ResponseParser.parseInt(cxn);
//                return 0;
//            }
//
//            public int sub(int a, int b) {
//                //HttpURLConnection cxn = newRequestBuilder("GET").path("sub", a, b).execute();
//                //return ResponseParser.parseInt(cxn);
//                return 0;
//            }
//
//            public int mul(int a, int b) {
//                //HttpURLConnection cxn = newRequestBuilder("GET").path("mul", a, b).execute();
//                //return ResponseParser.parseInt(cxn);
//                return 0;
//            }
//
//            private static HttpURLConnection newRequestBuilder(String method) { return null; }
//            private static final class ResponseParser {}
//        }
//    }
//
//
//    ////////////////////////////////////////////////////////////
//    // Code that service consumers need to write
//    ////////////////////////////////////////////////////////////
//    private static class Consumer {
//        private static void run() {
//            Services.executeWith(Provider.CalculatorClient.class, new ServiceCallback<Provider.CalculatorClient, Integer>() {
//                public Integer call(Provider.CalculatorClient client) {
//                    return client.add(1, client.sub(3, client.mul(1, 1)));
//                }
//            });
//
//            Services.executeWith(Provider.CalculatorClient.class, new ServiceCallbackNoResult<Provider.CalculatorClient>() {
//                public void call(Provider.CalculatorClient client) {
//                    client.add(1, 2);
//                }
//            });
//        }
//
//
//        private static void shawnRun() {
//            CalculatorClientConnectionPool pool = null;
//            pooll.executeWith(new ServiceCallback<Provider.CalculatorClient, Integer>() {
//                public Integer call(Provider.CalculatorClient client) {
//                    return client.add(1, client.sub(3, client.mul(1, 1)));
//                }
//            });
//        }
//    }
//
//
////    private static class JerseyClientFactory<T> implements ClientFactory<T> {
////        public JerseyClientFactory(Class<T> interfaceType) {
////        }
////        public T create(Properties propsFromZookeeper) {
////            return JerseyClient.provideImpl(_interfaceType, propsFromZookeeper.getProperty("hostname"));
////        }
////    }
//
//    // two types of client wrappers:
//    // - cxn oriented like Connection where we want to restrict the # that are created,
//    //   multiple threads can't share the same one
//    // - stateless (like http client) where the T is really just a URL that can be shared
//    //   across threads
//    // in both cases, if there's failure we assume the T is tainted until proven otherwise.
//    // - basically, ask the question is Properties->T 1-1 or 1-many (pool managed) or 1-0 (create and throw away on each use)

}
