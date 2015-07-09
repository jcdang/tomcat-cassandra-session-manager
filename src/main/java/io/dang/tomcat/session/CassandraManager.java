package io.dang.tomcat.session;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.session.PersistentManagerBase;
import org.apache.catalina.util.SessionIdGeneratorBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;

/**
 * https://tomcat.apache.org/tomcat-8.0-doc/config/manager.html
 */
public class CassandraManager extends PersistentManagerBase {
    private final Log log = LogFactory.getLog(CassandraManager.class);

    protected static final String NAME = "CassandraManager";


    public CassandraManager() {

        sessionIdGenerator = UuidSessionIdGenerator.getInstance();
        sessionIdGeneratorClass = UuidSessionIdGenerator.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Session createEmptySession() {
        return new CassandraSession(this);
    }

    @Override
    public String generateSessionId() {
        return sessionIdGenerator.generateSessionId();
    }

    @Override
    public void processExpires() {
        super.processExpires();

        // All Sessions loaded in memory
        Session[] sessions = findSessions();

        long timeNow = System.currentTimeMillis();

        for (Session session : sessions) {
            int timeIdle = (int) ((timeNow - session.getThisAccessedTime()) / 1000L);
            if (timeIdle < session.getMaxInactiveInterval()) {
                continue;
            }
            session.recycle();
            remove(session);
        }
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        super.stopInternal();
    }

    @Override
    public SessionIdGenerator getSessionIdGenerator() {
        return sessionIdGenerator;
    }

    @Override
    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        log.warn("SessionIdGenerator cannot be modified");
    }

    /**
     * UUID Session ID Generator UUIDs are required for this manager
     */
    public static class UuidSessionIdGenerator extends SessionIdGeneratorBase {

        public static final UuidSessionIdGenerator INSTANCE = new UuidSessionIdGenerator();

        private UuidSessionIdGenerator() {}

        @Override
        public String generateSessionId(String route) {
            return UUIDs.timeBased().toString();
        }

        public static UuidSessionIdGenerator getInstance() {
            return INSTANCE;
        }

        @Override
        public int getSessionIdLength() {
            // UUID length
            return 36;
        }
    }

}
