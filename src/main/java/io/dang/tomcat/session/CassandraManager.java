package io.dang.tomcat.session;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.catalina.Session;
import org.apache.catalina.session.PersistentManagerBase;

/**
 * https://tomcat.apache.org/tomcat-8.0-doc/config/manager.html
 */
public class CassandraManager extends PersistentManagerBase {
    protected static final String NAME = "CassandraManager";

    protected String keyspace;
    protected String tableName;

    public CassandraManager() {

    }

    @Override
    public Session createEmptySession() {
        return new CassandraSession(this);
    }

    @Override
    public String generateSessionId() {
        return UUIDs.timeBased().toString();
    }

}
