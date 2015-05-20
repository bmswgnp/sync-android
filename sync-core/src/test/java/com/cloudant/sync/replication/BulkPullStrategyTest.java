package com.cloudant.sync.replication;

import com.cloudant.common.RequireDBProxyEndpoint;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

/**
 * Replaces the BasicPullStrategy with a BulkPullStrategy and then runs the same set of tests as
 *  the BasicPullStrategy.
 */
@Category(RequireDBProxyEndpoint.class)
public class BulkPullStrategyTest extends BasicPullStrategyTest {

    @BeforeClass
    public static void setup(){
        //check if the test proxy has been set, and if so set it for the
        //BulkGetRevisionTask to use
        String proxy;
        if ((proxy = System.getProperty("test.cloudant.db.proxy")) != null){
            System.setProperty("cloudant.db.proxy", proxy);
        }
    }

    @Override
    protected void pull(Replication.Filter filter) throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        PullReplication pullReplication = this.createPullReplication();
        pullReplication.filter = filter;

        this.replicator = new BulkPullStrategy(pullReplication, null, this.config);
        this.replicator.getEventBus().register(listener);
        this.replicator.run();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }
}
