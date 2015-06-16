package com.cloudant.sync.replication;

import com.cloudant.common.CouchTestBase;
import com.cloudant.common.RequireDBProxyEndpoint;
import com.cloudant.mazha.CouchConfig;
import com.cloudant.sync.datastore.DatastoreExtended;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.util.TestUtils;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 */
@Category(RequireDBProxyEndpoint.class)
@RunWith(Parameterized.class)
public class BulkPullPerformanceTest extends CouchTestBase {

    public static int ITERATIONS = 10;
    public static int DOCS_TO_REPLICATE = 100;
    public static int DOC_UPDATES = 0;
    public static int CHANGES_LIMIT = 1000;

    protected static CouchClientWrapper remoteDb = null;
    private static final String remoteDbName = BulkPullPerformanceTest.class.getSimpleName()
            .toLowerCase(Locale.ENGLISH) +
            System.currentTimeMillis();

    private static FileWriter csv = null;

    public String datastoreManagerPath = null;
    protected DatastoreManager datastoreManager = null;
    protected DatastoreExtended datastore = null;

    static{
        //set the number of iterations, docs etc
        ITERATIONS = Integer.parseInt(System.getProperty("test.bulk.pull.iterations", Integer
                .toString(ITERATIONS)));
        DOCS_TO_REPLICATE = Integer.parseInt(System.getProperty("test.bulk.pull.docs", Integer
                .toString(DOCS_TO_REPLICATE)));
        DOC_UPDATES = Integer.parseInt(System.getProperty("test.bulk.pull" +
                ".updates", Integer.toString(DOC_UPDATES)));
        CHANGES_LIMIT = Integer.parseInt(System.getProperty("test.bulk.pull.changes.limit",
                Integer.toString(CHANGES_LIMIT)));
    }

    @Parameterized.Parameter
    public int runId;
    @Parameterized.Parameter(value = 1)
    public boolean bulk;

    @Parameterized.Parameters(name = "Run {0} bulk:{1}")
    public static Iterable<Object[]> data() {
        List<Object[]> parameters = new ArrayList<Object[]>();
        for (int i = 0; i < ITERATIONS; i++) {
            parameters.add(new Object[]{i, false});
            parameters.add(new Object[]{i, true});
        }
        return parameters;
    }

    @BeforeClass
    public static void setup() throws Exception {
        //check if the test proxy has been set, and if so set it for the
        //BulkGetRevisionTask to use
        String proxy;
        if ((proxy = System.getProperty("test.cloudant.db.proxy")) != null) {
            System.setProperty("cloudant.db.proxy", proxy);
        }

        csv = new FileWriter(new File("build/reports/bulkPullPerfData.csv"));
        csv.write("Run,isBulk,Duration (ms)\n");
    }

    @AfterClass
    public static void teardown() {
        CouchClientWrapperDbUtils.deleteDbQuietly(remoteDb);
        IOUtils.closeQuietly(csv);
    }

    @Before
    public void setUp() throws Exception {
        String datastoreForTest = ((bulk) ? "bulk" :
                "basic") + runId;
        datastoreManagerPath = TestUtils.createTempTestingDir(this.getClass().getName());
        datastoreManager = new DatastoreManager(this.datastoreManagerPath);
        datastore = (DatastoreExtended) datastoreManager.openDatastore(datastoreForTest);
        if (remoteDb == null) {
            this.createRemoteDB();
            populateRemoteDB(DOCS_TO_REPLICATE, DOC_UPDATES);
        }
    }

    @After
    public void tearDown() throws Exception {
        datastore.close();
        TestUtils.deleteTempTestingDir(datastoreManagerPath);
    }

    private List<String> generatedDocs = new ArrayList<String>();

    @Test
    public void performanceTest() throws Exception {
        long duration = pull(bulk);
        assertReplicationComplete();
        csv.write(runId + "," + bulk + "," + duration + "\n");
    }

    private void populateRemoteDB(int docsToGenerate, int revsToAdd) {
        generatedDocs.clear();
        Random r = new Random();
        for (int i = 0; i < docsToGenerate; i++) {
            String docId = UUID.randomUUID().toString();
            //create a doc
            BarUtils.createBar(remoteDb, docId, docId, r.nextInt(100));
            //add some revs
            for (int j = 0; j < revsToAdd; j++) {
                BarUtils.updateBar(remoteDb, docId, docId, r.nextInt(100));
            }
            generatedDocs.add(docId);
        }
    }

    private void assertReplicationComplete() throws Exception {
        for (String docId : generatedDocs) {
            Assert.assertNotNull(datastore.getDocument(docId));
        }
    }

    private long pull(boolean bulk) throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        PullReplication pullReplication = this.createPullReplication();
        PullConfiguration config = new PullConfiguration(CHANGES_LIMIT, PullConfiguration
                .DEFAULT_MAX_BATCH_COUNTER_PER_RUN, PullConfiguration.DEFAULT_INSERT_BATCH_SIZE,
                PullConfiguration.DEFAULT_PULL_ATTACHMENTS_INLINE);
        BasicPullStrategy replicator = bulk ? new BulkPullStrategy(pullReplication, null, config) :
                new
                        BasicPullStrategy(pullReplication, null, config);
        replicator.getEventBus().register(listener);
        long startTime = System.currentTimeMillis();
        replicator.run();
        long duration = System.currentTimeMillis() - startTime;
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
        return duration;
    }

    private PullReplication createPullReplication() throws URISyntaxException {
        PullReplication pullReplication = new PullReplication();
        CouchConfig couchConfig = this.getCouchConfig(remoteDbName);
        pullReplication.source = couchConfig.getRootUri();
        pullReplication.target = this.datastore;
        return pullReplication;
    }

    private void createRemoteDB() {
        remoteDb = new CouchClientWrapper(super.getCouchConfig(remoteDbName));
        remoteDb.createDatabase();
    }
}
