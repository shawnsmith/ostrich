Dictionary Example
==================
The dictionary example illustrates a simple server and client where servers can be configured to handle different
subsets of a dictionary and clients will route requests to the right set of servers. It illustrates embedding a
partition descriptor in the `ServiceEndPoint` payload and using an instance of
`com.bazaarvoice.soa.partition.PartitionFilter` to ensure requests are sent to the correct server.


Running the Example
--------------------

1.  Compile the code:

        $ mvn clean install

2.  Start ZooKeeper locally, listening on port 2181.

        $ vagrant up

3.  Create a minimal YAML configuration file:

        $ echo "{}" > /tmp/config.yaml

4.  In one window, start a dictionary server to handle words in the range `a-k`:

        $ cd examples/dictionary/service
        $ java -Ddw.wordRange=a-k -Ddw.http.port=8080 -Ddw.http.adminPort=8081 \
            -jar target/dictionary-service-*.jar server /tmp/config.yaml

5.  In another window, start a second dictionary server to handle words in the range `l-z`:

        $ cd examples/dictionary/service
        $ java -Ddw.wordRange=l-z -Ddw.http.port=8180 -Ddw.http.adminPort=8181 \
            -jar target/dictionary-service-*.jar server /tmp/config.yaml

6.  In another window, run a dictionary client to spell check a file:

        $ cd examples/dictionary/user
        $ java -jar target/dictionary-user-*.jar /tmp/config.yaml /usr/share/dict/README

Configuration
-------------
Ostrich uses [chameleon](https://github.com/bazaarvoice/chameleon) for setting the default ZooKeeper connect string.
The ZooKeeper connect string inferred by chameleon can be overridden in chameleon through an environment variable or
system property. The ZooKeeper connect string can be set in ostrich programmatically through the ZooKeeperConfiguration
object. In this example, the ZooKeeperConfiguration object is exposed through DropWizard and can be set via a YAML
configuration file.

### Setting ZooKeeper Connect String via Environment
Starting the dictionary server for `a-k` (Step 4 above):

	$ export CHAMELEON_ZOOKEEPER_ENSEMBLE=localhost:2181
    $ cd examples/dictionary/service
    $ java -Ddw.wordRange=a-k -Ddw.http.port=8080 -Ddw.http.adminPort=8081 \
        -jar target/dictionary-service-*.jar server /tmp/config.yaml
	
Starting the dictionary server for `l-z` (Step 5 above):

	$ export CHAMELEON_ZOOKEEPER_ENSEMBLE=localhost:2181
    $ cd examples/dictionary/service
    $ java -Ddw.wordRange=l-z -Ddw.http.port=8180 -Ddw.http.adminPort=8181 \
        -jar target/dictionary-service-*.jar server /tmp/config.yaml
	
Starting the dictionary client (Step 6 above)

	$ export CHAMELEON_ZOOKEEPER_ENSEMBLE=localhost:2181
    $ cd examples/dictionary/user
    $ java -jar target/dictionary-user-*.jar /tmp/config.yaml /usr/share/dict/README

### Setting ZooKeeper Connect String via System Property
Starting the dictionary server for `a-k` (Step 4 above):

    $ cd examples/dictionary/service
    $ java -Dchameleon.zookeeper.ensemble=localhost:2181 \
    	-Ddw.wordRange=a-k -Ddw.http.port=8080 -Ddw.http.adminPort=8081 \
        -jar target/dictionary-service-*.jar server /tmp/config.yaml
	
Starting the dictionary server for `l-z` (Step 5 above):

    $ cd examples/dictionary/service
    $ java -Dchameleon.zookeeper.ensemble=localhost:2181 \
    	-Ddw.wordRange=l-z -Ddw.http.port=8180 -Ddw.http.adminPort=8181 \
        -jar target/dictionary-service-*.jar server /tmp/config.yaml
	
Starting the dictionary client (Step 6 above)

    $ cd examples/dictionary/user
    $ java -Dchameleon.zookeeper.ensemble=localhost:2181 \
    	-jar target/dictionary-user-*.jar /tmp/config.yaml /usr/share/dict/README

### Setting ZooKeeper Connect String via YAML configuration file
The ZooKeeper connect string can also be set through the YAML configuration file. The definition of the server
configuration object can be found in `com.bazaarvoice.soa.examples.dictionary.service.DictionaryConfiguration`. The
definition of the user configuration object can be found in
`com.bazaarvoice.soa.examples.dictionary.user.DictionaryConfiguration`. The ZooKeeper connect string and namespace can
be set in the server and client configurations by defining the `"zooKeeper"` object.

```
{"zooKeeper": {
	"connectString":"localhost:2181",
	"namespace":"/examplenamespace"
	}
}
```
Please refer to the server and user `DictionaryConfiguration` definitions to see the available configuration options.