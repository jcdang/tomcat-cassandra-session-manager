package io.dang.tomcat;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class CassandraClient {

    protected final String REPLICATION = "WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};";
    protected final String KEYSPACE_EXISTS_CQL = "SELECT * FROM SYSTEM.SCHEMA_KEYSPACES WHERE KEYSPACE_NAME = ?";
    protected final String CREATE_KEYSPACE_CQL = "CREATE KEYSPACE %s " + REPLICATION;
    protected final String TABLE_EXISTS_CQL = "SELECT COLUMNFAMILY_NAME FROM SYSTEM.SCHEMA_COLUMNFAMILIES " +
                                              "WHERE KEYSPACE_NAME = ? AND COLUMNFAMILY_NAME = ?";

    private Cluster cluster;
    private Session cassandraSession;
    private String clusterName;
    private String keyspaceName;
    private String tableName = "sessions";

    public CassandraClient(String clusterName, String keyspaceName) {
        this.clusterName = clusterName;
        this.keyspaceName = keyspaceName;
    }

    public void connect(String nodes, int port) {
        if (nodes != null) {
            connect(nodes.split(","), port);
        }
    }
    public void connect(String[] nodes, int port) {
//        boolean createTable = false;
        cluster = Cluster.builder()
                .withClusterName(clusterName)
                .addContactPoints(nodes)
                .withPort(port)
                .build();
        Session globalSession = cluster.connect();

        doCreateKeyspace(globalSession, keyspaceName);

        try {
            if (!isKeyspacePresent(globalSession, keyspaceName)) {
                throw new IllegalStateException("Unable to create keyspace " + keyspaceName);
            }

        } finally {
            globalSession.close();
        }
        cassandraSession = cluster.connect(keyspaceName);
    }

    public void close() {
        if (cassandraSession != null) {
            cassandraSession.close();
        }

        if (cluster != null) {
            cluster.close();
        }
    }

    public Session getSession() {
        return cassandraSession;
    }

    /**
     * Tests to see if a table exists in context of the default keyspace.
     * @param tableName The table being checked
     * @return true if table is present false otherwise
     */
    public boolean isTablePresent(String tableName) {
        return isTablePresent(cassandraSession, keyspaceName, tableName);
    }


    /**
    * Tests to see if a table exists
    * @param session The cassandra session
    * @param keyspaceName the table's keyspace
    * @param tableName The table being checked
    * @return true if table is present and false otherwise
    */
    public boolean isTablePresent(Session session, String keyspaceName, String tableName) {
        if (session == null || keyspaceName == null || tableName == null)
            return false;

        ResultSet rs = session.execute(TABLE_EXISTS_CQL, keyspaceName, tableName);
        return rs.one() != null;
    }

    /**
     * Checks if a keyspace exist using the keyspace Session
     * @param keyspaceName the name of the keyspace
     * @return true if the keyspace is present and false otherwise
     */
    public boolean isKeyspacePresent(String keyspaceName) {
        return isKeyspacePresent(cassandraSession, keyspaceName);
    }

    /**
     * Checks if a keyspace exists using the passed in Session
     * @param session The cassandraSession to use
     * @param keyspaceName The name of the keyspace
     * @return true if the keyspace is present and false otherwise
     */
    public boolean isKeyspacePresent(Session session, String keyspaceName) {
        if (keyspaceName == null)
            return false;

        ResultSet rs = session.execute(KEYSPACE_EXISTS_CQL, keyspaceName);
        return rs.one() != null;
    }


    /**
     * Creates the specified keyspace with the keyspace Session if it doesn't already exist.
     * @param keyspaceName The name of the keyspace
     */
    public void doCreateKeyspace(String keyspaceName) {
        doCreateKeyspace(cassandraSession, keyspaceName);
    }

    /**
     * Creates the specified keyspace if it doesn't already exist
     * @param session The cassandraSession to use
     * @param keyspaceName The name of the keyspace
     */
    public void doCreateKeyspace(Session session, String keyspaceName) {
        if (session == null || keyspaceName == null)
            throw new IllegalArgumentException("Unable to create " + keyspaceName + "with session " + session);

        if (isKeyspacePresent(session, keyspaceName))
            return;

        session.execute(String.format(CREATE_KEYSPACE_CQL, keyspaceName));
    }
}
