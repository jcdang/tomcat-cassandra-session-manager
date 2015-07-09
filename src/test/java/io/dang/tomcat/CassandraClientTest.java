package io.dang.tomcat;

import org.scassandra.Scassandra;
import org.scassandra.ScassandraFactory;
import org.scassandra.http.client.ActivityClient;
import org.scassandra.http.client.PrimingClient;
import org.testng.annotations.*;

import static org.testng.Assert.*;

public class CassandraClientTest {
    protected static Scassandra scassandra;
    protected static int binaryPort = 8042;
    protected static int adminPort = 8043;
    protected static PrimingClient primingClient;
    protected static ActivityClient activityClient;

    @BeforeClass
    public static void start() {
           scassandra = ScassandraFactory.createServer(binaryPort, adminPort);
    }

    @Test
    public void clientTest() {
        CassandraClient client = new CassandraClient("TestCluster", "TestKeyspace");
        primingClient = scassandra.primingClient();
        activityClient = scassandra.activityClient();
        scassandra.start();
    }

    @AfterClass
    public static void stopScassandra() {
        scassandra.stop();
    }

    @BeforeMethod
    public void setup() {
        activityClient.clearAllRecordedActivity();
        primingClient.clearAllPrimes();
    }

    @AfterMethod
    public void tearDown() {

    }
}
