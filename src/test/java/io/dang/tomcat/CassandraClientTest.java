package io.dang.tomcat;

import com.google.common.collect.ImmutableMap;
import org.scassandra.Scassandra;
import org.scassandra.ScassandraFactory;
import org.scassandra.http.client.*;
import org.scassandra.matchers.Matchers;
import org.testng.annotations.*;

import java.util.List;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.scassandra.matchers.Matchers.containsQuery;
import static org.testng.Assert.*;
import static org.scassandra.cql.PrimitiveType.*;


public class CassandraClientTest {
    protected static Scassandra scassandra;
    protected static int binaryPort = 8042;
    protected static int adminPort = 8043;
    protected static PrimingClient primingClient;
    protected static ActivityClient activityClient;

    @BeforeClass
    public static void start() {
        scassandra = ScassandraFactory.createServer(binaryPort, adminPort);
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

    @Test
    public void connectionCreationTest() {
        CassandraClient cassandraClient = new CassandraClient("TestCluster", "TestKeyspace");

        String query = "SELECT * FROM SYSTEM.SCHEMA_KEYSPACES WHERE KEYSPACE_NAME = ?";
        Query expectedQuery = Query.builder().withQuery(query).withConsistency("ONE").build();

        Map<String, ?> row = ImmutableMap.of("dummy_row", "dummy_value");
        PrimingRequest prime = PrimingRequest.queryBuilder()
                .withQuery(query)
                .withRows(row)
                .withConsistency(PrimingRequest.Consistency.ONE)
                .build();

        primingClient.prime(prime);
        cassandraClient.connect("localhost", binaryPort);


        assertThat(activityClient.retrieveQueries(), containsQuery(expectedQuery));

        assertNotEquals(activityClient.retrieveConnections().size(), 0);

        cassandraClient.close();

    }
}
