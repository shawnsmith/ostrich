package com.bazaarvoice.ostrich.examples.dictionary.user;

import com.bazaarvoice.ostrich.discovery.zookeeper.ZooKeeperHostDiscovery;
import com.bazaarvoice.ostrich.dropwizard.healthcheck.ContainsHealthyEndPointCheck;
import com.bazaarvoice.ostrich.examples.dictionary.client.DictionaryService;
import com.bazaarvoice.ostrich.examples.dictionary.client.DictionaryServiceFactory;
import com.bazaarvoice.ostrich.pool.ServiceCachingPolicy;
import com.bazaarvoice.ostrich.pool.ServiceCachingPolicyBuilder;
import com.bazaarvoice.ostrich.pool.ServicePoolBuilder;
import com.bazaarvoice.ostrich.pool.ServicePoolProxies;
import com.bazaarvoice.ostrich.retry.ExponentialBackoffRetry;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.netflix.curator.framework.CuratorFramework;
import com.yammer.dropwizard.config.ConfigurationFactory;
import com.yammer.dropwizard.validation.Validator;
import com.yammer.metrics.HealthChecks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class DictionaryUser {
    private static final Logger LOG = LoggerFactory.getLogger(DictionaryUser.class);

    private static final CharMatcher LETTER = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));

    private final DictionaryService _service;

    public DictionaryUser(DictionaryService service) {
        _service = service;
    }

    public void spellCheck(File file) throws IOException {
        LOG.info("Spell checking file: {}", file);
        Files.readLines(file, Charsets.UTF_8, new LineProcessor<Void>() {
            @Override
            public boolean processLine(String line) throws IOException {
                for (String word : Splitter.on(CharMatcher.WHITESPACE).split(line)) {
                    // Discard punctuation, numbers, etc.
                    word = LETTER.retainFrom(word);
                    if (!word.isEmpty()) {
                        spellCheck(word);
                    }
                }
                return true;
            }

            @Override
            public Void getResult() {
                return null;
            }
        });
    }

    public void spellCheck(String word) {
        try {
            if (_service.contains(word.toLowerCase())) {
                LOG.info("ok: {}", word);
            } else {
                LOG.info("MISSPELLED: {}", word);
            }
        } catch (Exception e) {
            LOG.warn("word:{}, {}", word, e);
        }

        try {
            Thread.sleep(10);
        } catch(InterruptedException e) {
            // ignored
        }
    }

    public static void main(String[] args) throws Exception {
        checkArgument(args.length == 2);

        // Load the config.yaml file specified as the first argument.
        DictionaryConfiguration config = ConfigurationFactory
                .forClass(DictionaryConfiguration.class, new Validator())
                .build(new File(args[0]));

        CuratorFramework curator = config.getZooKeeperConfiguration().newCurator();
        curator.start();

        // Connection caching is optional, but included here for the sake of demonstration.
        ServiceCachingPolicy cachingPolicy = new ServiceCachingPolicyBuilder()
                .withMaxNumServiceInstances(10)
                .withMaxNumServiceInstancesPerEndPoint(1)
                .withMaxServiceInstanceIdleTime(5, TimeUnit.MINUTES)
                .build();

        // The service is partitioned, but partition filtering is configured by the ServiceFactory in this case
        // when the builder calls its configure() method.
        DictionaryServiceFactory serviceFactory = new DictionaryServiceFactory(config.getHttpClientConfiguration());
        DictionaryService service = ServicePoolBuilder.create(DictionaryService.class)
                .withServiceFactory(serviceFactory)
                .withHostDiscovery(new ZooKeeperHostDiscovery(curator, serviceFactory.getServiceName()))
                .withCachingPolicy(cachingPolicy)
                .buildProxy(new ExponentialBackoffRetry(5, 50, 1000, TimeUnit.MILLISECONDS));

        // If using Yammer Metrics or running in Dropwizard (which includes Yammer Metrics), you may want a health
        // check that pings a service you depend on. This will register a simple check that will confirm the service
        // pool contains at least one healthy end point.
        HealthChecks.register(new ContainsHealthyEndPointCheck(ServicePoolProxies.getPool(service), "dictionary-user"));

        DictionaryUser user = new DictionaryUser(service);
        for (int i = 1; i < args.length; i++) {
            user.spellCheck(new File(args[i]));
        }

        ServicePoolProxies.close(service);
        Closeables.closeQuietly(curator);
    }
}
