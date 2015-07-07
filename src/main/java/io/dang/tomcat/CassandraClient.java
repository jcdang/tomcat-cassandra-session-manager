package io.dang.tomcat;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

public class CassandraClient {

    protected final String REPLICATION = "WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};";
    protected final String KEYSPACE_EXISTS_CQL = "SELECT * FROM SYSTEM.SCHEMA_KEYSPACES WHERE KEYSPACE_NAME = ?";
    protected final String CREATE_KEYSPACE_CQL = "CREATE KEYSPACE %s " + REPLICATION;
    protected final String TABLE_EXISTS_CQL = "SELECT COLUMNFAMILY_NAME FROM SYSTEM.SCHEMA_COLUMNFAMILIES " +
                                              "WHERE KEYSPACE_NAME = ? AND COLUMNFAMILY_NAME = ?";
    protected final String CREATE_SESSION_TABLE_CQL =
            "CREATE TABLE %s (" +
            "    session_id     timeuuid PRIMARY KEY, " +
            "    app_name       text, " +
            "    valid_session  boolean, " +
            "    max_inactive   bigint, " +
            "    last_access    timestamp, " +
            "    session_data   blob " +
            ") " +
            "WITH " +
            "    gc_grace_seconds = 86400 AND " +
            "    compaction = {'class':'LeveledCompactionStrategy'};";
    protected final String UPSERT_TO_SESSION_TABLE_CQL =
            "UPDATE %s USING TTL ? " +
            "SET " +
            "   app_name = ? " +
            "   valid_session = ? " +
            "   max_inactive = ? " +
            "   last_access = ? " +
            "   session_data = ? " +
            "WHERE " +
            "   session_id = ?";
    protected final String DELETE_FROM_SESSION_TABLE_CQL = "DELETE FROM %s WHERE session_id = ?";


    private Cluster cluster;
    private Session cassandraSession;
    private String clusterName;
    private String keyspaceName;
    private String tableName;

    public CassandraClient(String clusterName, String keyspaceName, String tableName) {
        this.clusterName = clusterName;
        this.keyspaceName = keyspaceName;
        this.tableName = tableName;
    }

    public void connect(String nodes, int port) {
        if (nodes != null) {
            connect(nodes.split(","), port);
        }
    }
    public void connect(String[] nodes, int port) {
        boolean createTable = false;
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
            if (!isTablePresent(globalSession, keyspaceName, tableName)) {
                createTable = true;
            }
        } finally {
            globalSession.close();
        }
        cassandraSession = cluster.connect(keyspaceName);

        if (createTable) {
          doCreateSessionTable(cassandraSession, keyspaceName, tableName);
        }
    }

    public void close() {
        if (cassandraSession != null) {
            cassandraSession.close();
        }

        if (cluster != null) {
            cluster.close();
        }
    }


    public void doCreateSessionTable(String tableName) {
        doCreateSessionTable(cassandraSession, keyspaceName, tableName);
    }

    public void doCreateSessionTable(Session session, String keyspaceName, String tableName) {
        if (session == null || keyspaceName == null || tableName == null)
            throw new IllegalArgumentException("Unable to create table " + tableName);

        if (isTablePresent(session, keyspaceName, tableName))
            return;

        session.execute(String.format(CREATE_SESSION_TABLE_CQL, tableName));
    }

    public boolean isTablePresent(String tableName) {
        return isTablePresent(cassandraSession, keyspaceName, tableName);
    }


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
