package io.dang.tomcat;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.apache.catalina.startup.Tomcat;


import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

import static org.testng.Assert.*;

public class AppTest {

    protected final int TOMCAT_PORT = 8080;
    protected final int CASSANDRA_PORT = 1234;

    // import to get around the snakeyaml issue -- We don't actually need it
    org.apache.cassandra.config.Config config = new Config();
    Tomcat tomcat;
    String webAppPath = "src/test/webapp";


    @BeforeClass
    public void beforeClass() throws ConfigurationException, IOException, TTransportException {
        //EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra.yaml");
    }

    @BeforeMethod
    public void setUp() throws InterruptedException, LifecycleException, ServletException
    {
        //EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
        tomcat = new Tomcat();
        tomcat.setPort(TOMCAT_PORT);
        tomcat.setSilent(true);
        Context context = tomcat.addWebapp("", new File(webAppPath).getAbsolutePath());
        //context.setManager();
        tomcat.start();
    }

    @AfterMethod
    public void tearDown() throws LifecycleException
    {
        tomcat.stop();
    }

    @Test
    public void basicTest() {
        assertEquals(1, 1);
    }

    @Test(enabled = false)
    public void cassandraClientTest() {
        CassandraClient client = new CassandraClient("Test Cluster", "tomcat");
        client.connect("localhost", CASSANDRA_PORT);
        client.close();
    }

    @Test(enabled = true)
    public void tomcatClientTest() throws IOException
    {
        WebClient webClient = new WebClient();
        HtmlPage page = webClient.getPage("http://localhost:" + TOMCAT_PORT + "/index.html");
        String textContent = page.getBody().getTextContent();
        assertEquals(textContent, "Hello");
    }

}
