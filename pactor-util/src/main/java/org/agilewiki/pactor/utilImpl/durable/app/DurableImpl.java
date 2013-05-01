package org.agilewiki.pactor.utilImpl.durable.app;

import org.agilewiki.pactor.util.durable.AppendableBytes;
import org.agilewiki.pactor.util.durable.Factory;
import org.agilewiki.pactor.util.durable.PASerializable;
import org.agilewiki.pactor.util.durable.ReadableBytes;
import org.agilewiki.pactor.util.durable.app.Durable;
import org.agilewiki.pactor.util.durable.incDes.PAInteger;
import org.agilewiki.pactor.utilImpl.durable.incDes.IncDesImpl;

/**
 * A base class for applications, DurableImpl provides a durable tuple without an external interface.
 */
public class DurableImpl extends IncDesImpl implements Durable {
    /**
     * The size of the serialized data (exclusive of its length header).
     */
    private int _len;

    /**
     * An array of jid factories, one for each element in the tuple.
     */
    public Factory[] tupleFactories;

    /**
     * A tuple of actors.
     */
    protected PASerializable[] tuple;

    /**
     * Returns the element factories.
     *
     * @return An array of element factories.
     */
    protected Factory[] getTupleFactories() {
        if (tupleFactories != null)
            return tupleFactories;
        throw new IllegalStateException("tupleFactories is null");
    }

    @Override
    public void _iSetBytes(int i, byte[] bytes) {
        _initialize();
        PASerializable elementJid = createSubordinate(tupleFactories[i], bytes);
        PASerializable oldElementJid = _iGet(i);
        ((IncDesImpl) oldElementJid.getDurable()).setContainerJid(null);
        tuple[i] = elementJid;
        change(elementJid.getDurable().getSerializedLength() -
                oldElementJid.getDurable().getSerializedLength());
    }

    @Override
    public int _size() {
        return getTupleFactories().length;
    }

    @Override
    public PASerializable _iGet(int i) {
        _initialize();
        if (i < 0)
            i += _size();
        if (i < 0 || i >= _size())
            return null;
        return tuple[i];
    }

    @Override
    public PASerializable _resolvePathname(String pathname) {
        if (pathname.length() == 0) {
            throw new IllegalArgumentException("empty string");
        }
        int s = pathname.indexOf("/");
        if (s == -1)
            s = pathname.length();
        if (s == 0)
            throw new IllegalArgumentException("pathname " + pathname);
        String ns = pathname.substring(0, s);
        int n = 0;
        try {
            n = Integer.parseInt(ns);
        } catch (Exception ex) {
            throw new IllegalArgumentException("pathname " + pathname);
        }
        if (n < 0 || n >= _size())
            throw new IllegalArgumentException("pathname " + pathname);
        PASerializable jid = _iGet(n);
        if (s == pathname.length())
            return jid;
        return jid.getDurable().resolvePathname(pathname.substring(s + 1));
    }

    /**
     * Perform lazy initialization.
     */
    private void _initialize() {
        if (tuple != null)
            return;
        tupleFactories = getTupleFactories();
        ReadableBytes readableBytes = null;
        if (isSerialized()) {
            readableBytes = readable();
            _skipLen(readableBytes);
        }
        tuple = new PASerializable[_size()];
        int i = 0;
        _len = 0;
        while (i < _size()) {
            PASerializable elementJid = createSubordinate(tupleFactories[i], readableBytes);
            _len += elementJid.getDurable().getSerializedLength();
            tuple[i] = elementJid;
            i += 1;
        }
    }

    /**
     * Skip over the length at the beginning of the serialized data.
     *
     * @param readableBytes Holds the serialized data.
     */
    private void _skipLen(ReadableBytes readableBytes) {
        readableBytes.skip(PAInteger.LENGTH);
    }

    /**
     * Returns the size of the serialized data (exclusive of its length header).
     *
     * @param readableBytes Holds the serialized data.
     * @return The size of the serialized data (exclusive of its length header).
     */
    private int _loadLen(ReadableBytes readableBytes) {
        return readableBytes.readInt();
    }

    /**
     * Writes the size of the serialized data (exclusive of its length header).
     *
     * @param appendableBytes The object written to.
     */
    private void _saveLen(AppendableBytes appendableBytes) {
        appendableBytes.writeInt(_len);
    }

    /**
     * Returns the number of bytes needed to serialize the persistent data.
     *
     * @return The minimum size of the byte array needed to serialize the persistent data.
     */
    @Override
    public int getSerializedLength() {
        _initialize();
        return PAInteger.LENGTH + _len;
    }

    /**
     * Serialize the persistent data.
     *
     * @param appendableBytes The wrapped byte array into which the persistent data is to be serialized.
     */
    @Override
    protected void serialize(AppendableBytes appendableBytes) {
        _saveLen(appendableBytes);
        int i = 0;
        while (i < _size()) {
            _iGet(i).getDurable().save(appendableBytes);
            i += 1;
        }
    }

    /**
     * Load the serialized data into the JID.
     *
     * @param readableBytes Holds the serialized data.
     */
    @Override
    public void load(ReadableBytes readableBytes) {
        super.load(readableBytes);
        _len = _loadLen(readableBytes);
        tuple = null;
        readableBytes.skip(_len);
    }

    /**
     * Process a change in the persistent data.
     *
     * @param lengthChange The change in the size of the serialized data.
     */
    @Override
    public void change(int lengthChange) {
        _len += lengthChange;
        super.change(lengthChange);
    }
}