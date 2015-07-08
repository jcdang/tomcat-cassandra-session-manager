package io.dang.tomcat.session;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

public class CassandraSession extends StandardSession {

    public CassandraSession(Manager manager) {
        super(manager);
    }
}
