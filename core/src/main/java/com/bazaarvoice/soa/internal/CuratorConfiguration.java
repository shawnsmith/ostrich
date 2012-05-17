package com.bazaarvoice.soa.internal;

import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;

import java.io.IOException;

/**
 * <b>NOTE: This is an INTERNAL class to the SOA library.  You should not be using this directly!!!!</b>
 *
 * This class provides a ZooKeeperConfiguration that is backed by Netflix's Curator library.  This class is in an
 * internal package to avoid users from using it.  Curator should not appear anywhere in the public interface of the SOA
 * library.
 *
 * @see com.bazaarvoice.soa.zookeeper.ZooKeeperConfigurationBuilder
 */
public class CuratorConfiguration implements ZooKeeperConfiguration {
    private final String _connectString;
    private final RetryPolicy _retryPolicy;
    private CuratorFramework _curator;

    public CuratorConfiguration(String connectString, RetryPolicy retryPolicy) {
        _connectString = Preconditions.checkNotNull(connectString);
        _retryPolicy = Preconditions.checkNotNull(retryPolicy);
    }

    public synchronized CuratorFramework getCurator() {
        if (_curator == null) {
            try {
                _curator = CuratorFrameworkFactory.builder()
                        .connectString(_connectString)
                        .retryPolicy(_retryPolicy)
                        .build();
                _curator.start();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        return _curator;
    }
}
