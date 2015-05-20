package com.cloudant.sync.replication;

import com.cloudant.common.RequireRunningCouchDB;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import java.util.concurrent.Executors;

/**
 * Replaces the BasicPullStrategy with a BulkPullStrategy and then runs the same set of tests as
 * the BasicPullStrategy.
 */
@Category(RequireRunningCouchDB.class)
public class BulkPullStrategyTest2 extends BasicPullStrategyTest2 {

    @BeforeClass
    public static void setup() {
        //check if the test proxy has been set, and if so set it for the
        //BulkGetRevisionTask to use
        String proxy;
        if ((proxy = System.getProperty("test.cloudant.db.proxy")) != null) {
            System.setProperty("cloudant.db.proxy", proxy);
        }
    }

    @Override
    protected void sync() throws Exception {
        TestStrategyListener listener = new TestStrategyListener();

        BasicPullStrategy replicator = new BulkPullStrategy(this.createPullReplication());
        replicator.getEventBus().register(listener);

        Executors.newSingleThreadExecutor().submit(replicator).get();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }

}
