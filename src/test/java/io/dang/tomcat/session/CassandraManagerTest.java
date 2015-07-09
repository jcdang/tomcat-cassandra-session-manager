package io.dang.tomcat.session;

import io.dang.tomcat.session.CassandraManager;
import org.apache.catalina.Manager;
import org.apache.catalina.SessionIdGenerator;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.*;

public class CassandraManagerTest {

    ConcurrentHashSet<String> sessionIds = new ConcurrentHashSet<>();

    @Test(invocationCount = 200, threadPoolSize = 8)
    public void sessionIdGeneratorTest() {
        Manager manager = new CassandraManager();
        SessionIdGenerator sGenerator = manager.getSessionIdGenerator();

        String strUuid = sGenerator.generateSessionId();

        assertNotNull(strUuid);
        assertEquals(strUuid.length(), sGenerator.getSessionIdLength());
        String[] uuidSplit = strUuid.split("-");
        assertEquals(uuidSplit.length, 5, "UUIDs must have 5 parts");

        //8-4-4-4-12
        assertEquals(uuidSplit[0].length(), 8);
        assertEquals(uuidSplit[1].length(), 4);
        assertEquals(uuidSplit[2].length(), 4);
        assertEquals(uuidSplit[3].length(), 4);
        assertEquals(uuidSplit[4].length(), 12);

        assertFalse(sessionIds.contains(strUuid), "Id must not be previously generated");
        sessionIds.add(strUuid);

    }
}
