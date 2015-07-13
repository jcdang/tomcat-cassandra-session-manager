package io.dang.tomcat.session;

import org.apache.catalina.session.StandardSessionFacade;

import javax.servlet.http.HttpSession;

public class CassandraSessionFacade extends StandardSessionFacade {


    protected transient boolean needToPersist = false;
    /**
     * Construct a new session facade.
     *
     * @param session the actual HttpSession
     */
    public CassandraSessionFacade(HttpSession session) {
        super(session);
    }

    @Override
    public Object getAttribute(String name) {
        needToPersist = true;
        return super.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        needToPersist = true;
        super.setAttribute(name, value);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        needToPersist = true;
        super.setMaxInactiveInterval(interval);
    }

    @Override
    public void removeAttribute(String name) {
        needToPersist = true;
        super.removeAttribute(name);
    }

    @Override
    public void invalidate() {
        needToPersist = true;
        super.invalidate();
    }

    public void resetNeedToPersist() {
        needToPersist = false;
    }

    public boolean isNeedToPersist() {
        return needToPersist;
    }

    public void setNeedToPersist(boolean needToPersist) {
        this.needToPersist = needToPersist;
    }
}
