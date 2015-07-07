package io.dang.tomcat;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

public class CassandraClient {

    protected final String REPLICATION = "WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};";
    protected final String KEYSPACE_EXISTS_CQL = "SELECT * FROM SYSTEM.SCHEMA_KEYSPACES WHERE KEYSPACE_NAME = ?";
    protected final String CREATE_KEYSPACE_CQL = "CREATE KEYSPACE %s " + REPLICATION;

    private Cluster cluster;
    private Session session;
    private String clusterName;

    public CassandraClient(String clusterName) {
        this.clusterName = clusterName;
    }

    public void connect(String nodes, int port) {
        if (nodes != null) {
            connect(nodes.split(","), port);
        }
    }
    public void connect(String[] nodes, int port) {
        cluster = Cluster.builder()
                .withClusterName(clusterName)
                .addContactPoints(nodes)
                .withPort(port)
                .build();
        session = cluster.connect();
    }

    public void close() {
        if (session != null) {
            session.close();
        }

        if (cluster != null) {
            cluster.close();
        }
    }

    public boolean isKeyspaceCreated(String keyspaceName) {
        ResultSet rs = session.execute(KEYSPACE_EXISTS_CQL, keyspaceName);
        return rs.one() != null;
    }

    public void doCreateKeyspace(String keyspaceName) {
        if (isKeyspaceCreated(keyspaceName))
            return;

        session.execute(String.format(CREATE_KEYSPACE_CQL, keyspaceName));
    }
}
