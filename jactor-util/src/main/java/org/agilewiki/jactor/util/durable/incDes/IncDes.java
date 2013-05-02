package org.agilewiki.jactor.util.durable.incDes;

import org.agilewiki.jactor.api.Actor;
import org.agilewiki.jactor.api.Mailbox;
import org.agilewiki.jactor.api.Request;
import org.agilewiki.jactor.util.Ancestor;
import org.agilewiki.jactor.util.durable.Factory;
import org.agilewiki.jactor.util.durable.JASerializable;
import org.agilewiki.jactor.utilImpl.durable.ReadableBytes;

public interface IncDes extends JASerializable, Actor, Ancestor {

    public static final String FACTORY_NAME = "incdes";

    Request<Integer> getSerializedLengthReq();

    /**
     * Returns the number of bytes needed to serialize the persistent data.
     *
     * @return The minimum size of the byte array needed to serialize the persistent data.
     */
    int getSerializedLength()
            throws Exception;

    Request<byte[]> getSerializedBytesReq();

    byte[] getSerializedBytes()
            throws Exception;

    Request<Integer> getSerializedBytesReq(byte[] bytes, int offset);

    int save(byte[] bytes, int offset)
            throws Exception;

    /**
     * Load the serialized data into the JID.
     *
     * @param _readableBytes Holds the serialized data.
     */
    void load(final ReadableBytes _readableBytes)
            throws Exception;

    void load(final byte[] _bytes) throws Exception;

    int load(final byte[] _bytes, final int _offset, final int _length)
            throws Exception;

    Request<JASerializable> resolvePathnameReq(final String _pathname);

    /**
     * Resolves a JID pathname, returning a JID actor or null.
     *
     * @param _pathname A JID pathname.
     * @return A JID actor or null.
     */
    JASerializable resolvePathname(final String _pathname)
            throws Exception;

    /**
     * Returns the factory.
     *
     * @return The factory, or null.
     */
    Factory getFactory();

    /**
     * Returns the jid type.
     *
     * @return The jid type, or null.
     */
    String getType();

    Request<JASerializable> copyReq(final Mailbox _m);

    JASerializable copy(final Mailbox m) throws Exception;

    Request<Boolean> isEqualReq(final JASerializable _jidA);
}