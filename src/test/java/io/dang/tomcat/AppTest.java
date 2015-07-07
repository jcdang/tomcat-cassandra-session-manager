package io.dang.tomcat;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.net.MessagingService;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

public class AppTest {

    // import to get around the snakeyaml issue -- We don't actually need it
    org.apache.cassandra.config.Config config = new Config();

    @BeforeMethod
    public void setUp() throws InterruptedException, TTransportException, ConfigurationException, IOException {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra.yaml");
    }

    @AfterMethod
    public void tearDown() {
        // EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Test
    public void basicTest() {
        assertEquals(1, 1);
    }

    @Test
    public void clientTest() {
        CassandraClient client = new CassandraClient("Test Cluster", "tomcat", "tomcat_sessions");
        client.connect("localhost", 1234);
        client.close();
    }
}
