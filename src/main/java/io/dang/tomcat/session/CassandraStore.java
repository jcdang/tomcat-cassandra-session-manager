package io.dang.tomcat.session;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.*;
import io.dang.tomcat.CassandraClient;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import org.apache.catalina.util.CustomObjectInputStream;

public class CassandraStore extends StoreBase {

    private String sessionTableName = "sessions";

    protected String sessionIdCol = "id";

    protected String sessionValidCol = "is_valid";

    protected String sessionMaxInactiveCol = "max_inactive";

    protected String sessionLastAccessedCol = "last_accessed";

    protected String sessionDataCol = "session_data";

    protected String clusterName = "TestCluster";
    protected String keyspace = "tomcat";
    protected String nodes = "localhost";
    protected int nativePort = 9042;


    final Object clientLock = new Object();

    CassandraClient client = null;

    protected final String CREATE_SESSION_TABLE_CQL =
            "CREATE TABLE %s (" +
                    "    %s     timeuuid PRIMARY KEY, " +
                    "    %s     boolean, " +
                    "    %s     bigint, " +
                    "    %s     timestamp, " +
                    "    %s     blob " +
                    ") " +
                    "WITH " +
                    "    gc_grace_seconds = 86400 AND " +
                    "    compaction = {'class':'LeveledCompactionStrategy'};";



    protected CassandraClient getClient() {
        if (client == null) {
            synchronized (clientLock) {
                if (client == null) {
                    client = new CassandraClient(clusterName, keyspace);
                    client.connect(nodes, nativePort);
                    doCreateSessionTable();
                }
                return client;
            }
        }
        return client;
    }

    /**
     * Creates the session table if the table doesn't already exist.
     */
    public void doCreateSessionTable() {

        if (getClient().isTablePresent(sessionTableName))
            return;

        getClient().getSession().execute(String.format(CREATE_SESSION_TABLE_CQL,
                sessionTableName,
                sessionIdCol,
                sessionValidCol,
                sessionMaxInactiveCol,
                sessionLastAccessedCol,
                sessionDataCol));
    }

    @Override
    public int getSize() throws IOException {
        Select select = QueryBuilder.select().countAll().from(sessionTableName);
        ResultSet rs = getClient().getSession().execute(select);
        Row row = rs.one();

        return row.getInt(0);
    }

    @Override
    public String[] keys() throws IOException {
        Select select = QueryBuilder.select(sessionIdCol).from(sessionTableName);
        ResultSet rs = getClient().getSession().execute(select);

        List<Row> rows = rs.all();
        String[] retArray = new String[rows.size()];

        int i = 0;
        for (Row row : rows) {
            UUID uuid = row.getUUID(0);
            retArray[i++] = uuid.toString();
        }

        return retArray;
    }

    @Override
    public StandardSession load(String id) throws ClassNotFoundException, IOException {
        StandardSession session = (StandardSession) getManager().createEmptySession();

        Select select = QueryBuilder.select(sessionIdCol, sessionDataCol)
                .from(sessionTableName)
                .where(QueryBuilder.eq(sessionIdCol, UUID.fromString(id)))
                .limit(1);

        ResultSet rs = client.getSession().execute(select);

        Row row = rs.one();

        if (row == null) {
            return null;
        }

        Context context = manager.getContext();
        ByteBuffer byteBuffer = row.getBytes(1);
        ByteArrayInputStream bis = new ByteArrayInputStream(byteBuffer.array());
        Loader loader = null;
        ClassLoader classLoader = null;
        ObjectInputStream ois;

        if (context != null) {
            loader = context.getLoader();
        }
        if (loader != null) {
            classLoader = loader.getClassLoader();
        }
        if (classLoader != null) {
            ois = new CustomObjectInputStream(bis, classLoader);
        } else {
            ois = new ObjectInputStream(bis);
        }

        session.setId(id);
        session.readObjectData(ois);

        try {
            ois.close();
        } catch (IOException ignore) {}


        return session;
    }

    @Override
    public void remove(String id) throws IOException {
        UUID uuid = UUID.fromString(id);
        Delete delete = QueryBuilder.delete()
                .from(sessionTableName)
                .where(QueryBuilder.eq(sessionIdCol, uuid))
                .ifExists();
        getClient().getSession().execute(delete);
    }

    @Override
    public void clear() throws IOException {
        Truncate truncate = QueryBuilder.truncate(sessionTableName);
        getClient().getSession().execute(truncate);
    }

    @Override
    public void processExpires() {
        // NOOP
        // Note: Sessions in Cassandra will expire by themselves with TTL.
        // We only need to get rid of the sessions in memory
    }

    @Override
    public void save(Session session) throws IOException {
        StandardSession cSession = (StandardSession) session;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);

        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            cSession.writeObjectData(oos);
        }

        Insert insert = QueryBuilder.insertInto(sessionTableName)
                .using(QueryBuilder.ttl(session.getMaxInactiveInterval()))
                .value(sessionIdCol, UUID.fromString(session.getId()))
                .value(sessionDataCol, ByteBuffer.wrap(baos.toByteArray()))
                .value(sessionValidCol, cSession.isValid())
                .value(sessionMaxInactiveCol, session.getMaxInactiveInterval())   // probably don't need this column
                .value(sessionLastAccessedCol, session.getLastAccessedTime());

        getClient().getSession().execute(insert);
    }


    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        String oldClusterName = this.clusterName;
        this.clusterName = clusterName;
        support.firePropertyChange("clusterName", oldClusterName, this.clusterName);
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        String oldKeyspace = this.keyspace;
        this.keyspace = keyspace;
        support.firePropertyChange("keyspace", oldKeyspace, this.keyspace);
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        String oldNodes = this.nodes;
        this.nodes = nodes;
        support.firePropertyChange("nodes", oldNodes, this.nodes);
    }

    public int getNativePort() { return nativePort; }

    public void setNativePort(int nativePort) {
        int oldNativePort = this.nativePort;
        this.nativePort = nativePort;
        support.firePropertyChange("nativePort", oldNativePort, this.nativePort);
    }
}
