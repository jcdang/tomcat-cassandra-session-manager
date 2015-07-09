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

    protected String clusterName;
    protected String keyspace;
    protected String nodes;

    CassandraClient client = new CassandraClient("dummy", "dummy");

    protected final String CREATE_SESSION_TABLE_CQL =
            "CREATE TABLE %s (" +
                    "    session_id     timeuuid PRIMARY KEY, " +
                    "    valid_session  boolean, " +
                    "    max_inactive   bigint, " +
                    "    last_access    timestamp, " +
                    "    session_data   blob " +
                    ") " +
                    "WITH " +
                    "    gc_grace_seconds = 86400 AND " +
                    "    compaction = {'class':'LeveledCompactionStrategy'};";

    /**
     * Creates the session table if the table doesn't already exist.
     */
    public void doCreateSessionTable() {

        if (client.isTablePresent(sessionTableName))
            return;

        client.getSession().execute(String.format(CREATE_SESSION_TABLE_CQL, sessionTableName));
    }


    @Override
    public int getSize() throws IOException {
        Select select = QueryBuilder.select().countAll().from(sessionTableName);
        ResultSet rs = client.getSession().execute(select);
        Row row = rs.one();

        return row.getInt(0);
    }

    @Override
    public String[] keys() throws IOException {
        Select select = QueryBuilder.select(sessionIdCol).from(sessionTableName);
        ResultSet rs = client.getSession().execute(select);

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
        client.getSession().execute(delete);
    }

    @Override
    public void clear() throws IOException {
        Truncate truncate = QueryBuilder.truncate(sessionTableName);
        client.getSession().execute(truncate);
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

        client.getSession().execute(insert);
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

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }
}
