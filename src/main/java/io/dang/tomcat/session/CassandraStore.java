package io.dang.tomcat.session;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.*;
import io.dang.tomcat.CassandraClient;
import io.dang.tomcat.io.ByteBufferInputStream;
import io.dang.tomcat.io.ByteBufferOutputStream;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.session.StoreBase;

import java.io.*;
import java.util.List;
import java.util.UUID;

import org.apache.catalina.util.CustomObjectInputStream;

public class CassandraStore extends StoreBase {

    private String sessionTableName = "sessions";

    protected String sessionIdCol = "id";

    protected String sessionDataCol = "session_data";

    protected String sessionValidCol = "is_valid";

    protected String sessionMaxInactiveCol = "max_inactive";

    protected String sessionLastAccessedCol = "last_accessed";

    protected String clusterName;
    protected String keyspace;
    protected String tableName;
    protected String nodes;

    CassandraClient client = new CassandraClient("dummy", "dummy");

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
    public CassandraSession load(String id) throws ClassNotFoundException, IOException {
        CassandraSession session = (CassandraSession) getManager().createEmptySession();

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
        ByteBufferInputStream bis = new ByteBufferInputStream(row.getBytes(1));
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
        CassandraSession cSession = (CassandraSession) session;
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(bbos);

        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            cSession.writeObjectData(oos);
        }

        Insert insert = QueryBuilder.insertInto(sessionTableName)
                .using(QueryBuilder.ttl(session.getMaxInactiveInterval()))
                .value(sessionIdCol, UUID.fromString(session.getId()))
                .value(sessionDataCol, bbos.getByteBuffer())
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
