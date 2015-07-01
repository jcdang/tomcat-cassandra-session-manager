package io.dang.tomcat.session;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.catalina.Session;
import org.apache.catalina.session.PersistentManagerBase;

/**
 * https://tomcat.apache.org/tomcat-8.0-doc/config/manager.html
 */
public class CassandraManager extends PersistentManagerBase {
    protected static final String NAME = "CassandraManager";




    protected String clusterName;
    protected String keyspace;
    protected String tableName;
    protected String nodes;

    public CassandraManager() {

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
        return UUIDs.timeBased().toString();
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

}
