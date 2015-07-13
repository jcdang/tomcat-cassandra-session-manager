package io.dang.tomcat.session;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.Store;
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
    public Session createEmptySession() {
        return new CassandraSession(this);
    }

    @Override
    public String getName() {
        return NAME;
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
            if (session.isValid()) {
                int timeIdle = (int) ((timeNow - session.getThisAccessedTime()) / 1000L);
                if (timeIdle < session.getMaxInactiveInterval()) {
                    continue;
                }
            }
            session.recycle();
            remove(session);
        }
    }

    /**
     * Adds only to the active session
     * @param session
     */
    @Override
    public void add(Session session) {
        // this will add it to active
        super.add(session);

        // this will persist it
/*        try {
            store.save(session);
        } catch (IOException e) {
            log.error("Failed to save session");
        }*/
    }

    @Override
    public Session createSession(String sessionId) {
        Session session = super.createSession(sessionId);
        add(session);
        return session;
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        if (store instanceof CassandraStore) {
            CassandraStore cs = (CassandraStore) store;
            cs.close();
        }
        super.destroyInternal();
    }

    @Override
    public SessionIdGenerator getSessionIdGenerator() {
        return sessionIdGenerator;
    }

    @Override
    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        log.warn("SessionIdGenerator cannot be modified");
    }

    @Override
    public void setStore(Store store) {
        if (!(store instanceof CassandraStore)) {
            throw new IllegalArgumentException("This store must derived from CassandraStore");
        }
        super.setStore(store);
    }

    /**
     * UUID Session ID Generator UUIDs are required for this manager
     */
    public static class UuidSessionIdGenerator extends SessionIdGeneratorBase {

        private UuidSessionIdGenerator() {}

        @Override
        public String generateSessionId(String route) {
            return UUIDs.timeBased().toString();
        }

        public static UuidSessionIdGenerator getInstance() {
            return new UuidSessionIdGenerator();
        }

        @Override
        public int getSessionIdLength() {
            // UUID length
            return 36;
        }
    }

}
