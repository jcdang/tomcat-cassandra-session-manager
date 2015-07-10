package io.dang.tomcat.session;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.io.ByteStreams;
import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Mohit
 * Date: 7/9/15
 * Time: 6:06 PM
 */

public class CassandraSession extends StandardSession {
    /**
     * Construct a new Session associated with the specified Manager.
     *
     * @param manager The manager with which this Session is associated
     */
    public CassandraSession(Manager manager) {
        super(manager);
    }


    @Override
    public void setId(String id, boolean notify) {

//        if ((this.id != null) && (manager != null))
//            manager.remove(this);

        this.id = id;

//        if (manager != null)
//            manager.add(this);

        if (notify) {
            tellNew();
        }
    }


    /**
     * Read a serialized version of this session object from the specified
     * object input stream.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The reference to the owning Manager
     * is not restored by this method, and must be set explicitly.
     *
     * @param stream The input stream to read from
     * @throws ClassNotFoundException if an unknown class is specified
     * @throws IOException            if an input/output error occurs
     */
    protected void doReadObject(ObjectInputStream stream)
            throws ClassNotFoundException, IOException {

        byte[] bytes = ByteStreams.toByteArray(stream);
        SessionData sessionData = (SessionData) read(bytes);
        // Deserialize the scalar instance variables (except Manager)
        authType = null;        // Transient only
        creationTime = sessionData.creationTime;
        lastAccessedTime = sessionData.lastAccessedTime;
        maxInactiveInterval = sessionData.maxInactiveInterval;
        isNew = sessionData.isNew;

        //TODO: Base class has a different logic to set this value. Test out multiple scenarios
        isValid = sessionData.isValid;
        thisAccessedTime = sessionData.thisAccessedTime;
        principal = null;        // Transient only
        id = sessionData.id;
        if (manager.getContext().getLogger().isDebugEnabled())
            manager.getContext().getLogger().debug
                    ("readObject() loading session " + id);

        List<String> attributeNames = sessionData.attributeNames;
        List<Object> attributeValues = sessionData.attributeValues;

        if (attributes == null)
            attributes = new ConcurrentHashMap<>();

        for (int i = 0; i < attributeNames.size(); i++) {
            String name = attributeNames.get(i);
            Object value = attributeValues.get(i);
            if ((value instanceof String) && (value.equals(NOT_SERIALIZED)))
                continue;
            if (manager.getContext().getLogger().isDebugEnabled())
                manager.getContext().getLogger().debug("  loading attribute '" + name +
                        "' with value '" + value + "'");
            attributes.put(name, value);
        }
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        if (notes == null) {
            notes = new Hashtable<>();
        }
    }

    /**
     * Write a serialized version of this session object to the specified
     * object output stream.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The owning Manager will not be stored
     * in the serialized representation of this Session.  After calling
     * <code>readObject()</code>, you must set the associated Manager
     * explicitly.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  Any attribute that is not Serializable
     * will be unbound from the session, with appropriate actions if it
     * implements HttpSessionBindingListener.  If you do not want any such
     * attributes, be sure the <code>distributable</code> property of the
     * associated Manager is set to <code>true</code>.
     *
     * @param stream The output stream to write to
     * @throws IOException if an input/output error occurs
     */
    protected void doWriteObject(ObjectOutputStream stream) throws IOException {

        // Write the scalar instance variables (except Manager)
        SessionData sessionData = new SessionData();
        sessionData.creationTime = creationTime;
        sessionData.lastAccessedTime = lastAccessedTime;
        sessionData.maxInactiveInterval = maxInactiveInterval;
        sessionData.isNew = isNew;
        sessionData.isValid = isValid;
        sessionData.thisAccessedTime = thisAccessedTime;
        sessionData.id = id;
        if (manager.getContext().getLogger().isDebugEnabled())
            manager.getContext().getLogger().debug
                    ("writeObject() storing session " + id);

        // Accumulate the names of serializable and non-serializable attributes
        String keys[] = keys();
        List<String> attributeNames = new ArrayList<>();
        List<Object> attributeValues = new ArrayList<>();
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value != null) {
                if (value instanceof Serializable && !exclude(key)) {
                    attributeNames.add(key);
                    attributeValues.add(value);
                } else {
                    removeAttributeInternal(key, true);
                }
            }
        }
        sessionData.attributeNames = attributeNames;
        sessionData.attributeValues = attributeValues;
        byte[] bytes = write(sessionData);
        stream.write(bytes);
    }

    public static final class SessionData {
        /**
         * The time this session was created, in milliseconds since midnight,
         * January 1, 1970 GMT.
         */
        protected long creationTime = 0L;
        /**
         * The last accessed time for this Session.
         */
        protected volatile long lastAccessedTime = creationTime;
        /**
         * The maximum time interval, in seconds, between client requests before
         * the servlet container may invalidate this session.  A negative time
         * indicates that the session should never time out.
         */
        protected int maxInactiveInterval = -1;
        /**
         * Flag indicating whether this session is new or not.
         */
        protected boolean isNew = false;
        /**
         * Flag indicating whether this session is valid or not.
         */
        protected volatile boolean isValid = false;
        /**
         * The current accessed time for this session.
         */
        protected volatile long thisAccessedTime = creationTime;
        /**
         * The session identifier of this Session.
         */
        protected String id = null;
        /**
         * The collection of user data attributes associated with this Session.
         */
        protected List<String> attributeNames;
        protected List<Object> attributeValues;
    }

    //*********
    // internal
    //*********

    private static final KryoPool pool = new KryoPool.Builder(() -> {
        Kryo kryo = new Kryo();
        kryo.register(CassandraSession.SessionData.class);
        return kryo;
    }).softReferences().build();

    private static Object read(byte[] bytes) {
        return pool.run(kryo -> kryo.readClassAndObject(new Input(bytes)));
    }

    private static byte[] write(Object obj) {
        return pool.run(kryo -> {
            Output output = new Output(4096, Integer.MAX_VALUE);
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        });
    }
}
