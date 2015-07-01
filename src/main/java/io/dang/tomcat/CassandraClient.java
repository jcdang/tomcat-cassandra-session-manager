package io.dang.tomcat;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraClient {
    private Cluster cluster;
    private Session session;
    private String clusterName;

    public CassandraClient(String clusterName) {
        this.clusterName = clusterName;
    }

    public void connect(String nodes) {
        if (nodes != null) {
            connect(nodes.split(","));
        }
    }
    public void connect(String[] nodes) {
        cluster = Cluster.builder()
                .withClusterName(clusterName)
                .addContactPoints(nodes)
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
}
