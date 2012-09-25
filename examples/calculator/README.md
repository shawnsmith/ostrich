Calculator Example
==================
The calculator example illustrates a very simple [Dropwizard] (http://dropwizard.codahale.com/)-based server and client
that uses ZooKeeper-based service registration and host discovery.


Running the Example
--------------------

1.  Compile the code:

        $ mvn clean install

2.  Start ZooKeeper locally, listening on port 2181.

        $ vagrant up

3.  Create a minimal YAML configuration file:

        $ echo "{}" > /tmp/serverconfig.yaml
        $ echo "{}" > /tmp/userconfig.yaml

4.  In one window, start a calculator server (See notes below on configuration):

        $ cd examples/calculator/service
        $ java -jar target/calculator-service-*.jar server /tmp/serverconfig.yaml

5.  In another window, start a calculator client (See notes below on configuration):

        $ cd examples/calculator/user
        $ java -jar target/calculator-user-*.jar /tmp/userconfig.yaml

6.  Simulate server failure:

        $ curl http://localhost:8080/health/500

    Watch the calculator client begin to receive errors.  If you start a 2nd calculator server on different ports
    the client should failover to the healthy server.

        $ curl http://localhost:8080/health/200

    Watch the calculator client recover.

7.  Start the alternate client that uses dynamic service proxies:

        $ java -classpath target/calculator-user-*.jar com.bazaarvoice.soa.examples.calculator.user.CalculatorProxyUser

Configuration
-------------
Ostrich uses [chameleon](https://github.com/bazaarvoice/chameleon) for setting the default ZooKeeper connect string.
The ZooKeeper connect string inferred by chameleon can be overridden in chameleon through an environment variable or
system property. The ZooKeeper connect string can be set in ostrich programmatically through the ZooKeeperConfiguration
object. In this example, the ZooKeeperConfiguration object is exposed through DropWizard and can be set via a YAML
configuration file.

### Setting ZooKeeper Connect String via Environment
Starting the calculator server (Step 4 above):

	$ export CHAMELEON_ZOOKEEPER_ENSEMBLE=localhost:2181
	$ cd examples/calculator/service
	$ java -jar target/calculator-service-*.jar server /tmp/config.yaml
	
Starting the calculator client (Step 5 above)

	$ export CHAMELEON_ZOOKEEPER_ENSEMBLE=localhost:2181
	$ cd examples/calculator/user
	$ java -jar target/calculator-user-*.jar

### Setting ZooKeeper Connect String via System Property
Starting the calculator server (Step 4 above):

	$ cd examples/calculator/service
	$ java -Dchameleon.zookeeper.ensemble=localhost:2181 -jar target/calculator-service-*.jar server
		tmp/config.yaml
	
Starting the calculator client (Step 5 above)

	$ cd examples/calculator/user
	$ java -Dchameleon.zookeeper.ensemble=localhost:2181 -jar target/calculator-user-*.jar

### Setting ZooKeeper Connect String via YAML configuration file
The ZooKeeper connect string can also be set through the YAML configuration file. The definition of the server
configuration object can be found in `com.bazaarvoice.soa.examples.calculator.service.CalculatorConfiguration`. The
definition of the user configuration object can be found in
`com.bazaarvoice.soa.examples.calculator.user.CalculatorConfiguration`. The ZooKeeper connect string and namespace can
be set in the server and client configurations by defining the `"zooKeeper"` object.

```
{"zooKeeper": {
	"connectString":"localhost:2181",
	"namespace":"/examplenamespace"
	}
}
```
Please refer to the server and user `CalculatorConfiguration` definitions to see the available configuration options.