package org.agilewiki.jactor2.utilImpl.durable.incDes.scalar.flens;

import org.agilewiki.jactor2.api.BoundRequest;
import org.agilewiki.jactor2.api.BoundRequestBase;
import org.agilewiki.jactor2.api.Mailbox;
import org.agilewiki.jactor2.api.Transport;
import org.agilewiki.jactor2.util.Ancestor;
import org.agilewiki.jactor2.util.durable.FactoryLocator;
import org.agilewiki.jactor2.util.durable.FactoryLocatorClosedException;
import org.agilewiki.jactor2.util.durable.incDes.JALong;
import org.agilewiki.jactor2.utilImpl.durable.AppendableBytes;
import org.agilewiki.jactor2.utilImpl.durable.FactoryImpl;
import org.agilewiki.jactor2.utilImpl.durable.FactoryLocatorImpl;
import org.agilewiki.jactor2.utilImpl.durable.ReadableBytes;

/**
 * A JID actor that holds a long.
 */
public class JALongImpl
        extends FLenScalar<Long> implements JALong {

    public static void registerFactory(FactoryLocator _factoryLocator) throws FactoryLocatorClosedException {
        ((FactoryLocatorImpl) _factoryLocator).registerFactory(new FactoryImpl(JALong.FACTORY_NAME) {
            @Override
            final protected JALongImpl instantiateActor() {
                return new JALongImpl();
            }
        });
    }

    private BoundRequest<Long> getLongReq;

    @Override
    public BoundRequest<Long> getValueReq() {
        return getLongReq;
    }

    /**
     * Create the value.
     *
     * @return The default value
     */
    @Override
    protected Long newValue() {
        return new Long(0L);
    }

    /**
     * Returns the value held by this component.
     *
     * @return The value held by this component.
     */
    @Override
    public Long getValue() {
        if (value != null)
            return value;
        ReadableBytes readableBytes = readable();
        value = readableBytes.readLong();
        return value;
    }

    @Override
    public BoundRequest<Void> setValueReq(final Long _v) {
        return new BoundRequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport rp) throws Exception {
                setValue(_v);
                rp.processResponse(null);
            }
        };
    }

    /**
     * Returns the number of bytes needed to serialize the persistent data.
     *
     * @return The minimum size of the byte array needed to serialize the persistent data.
     */
    @Override
    public int getSerializedLength() {
        return LENGTH;
    }

    /**
     * Serialize the persistent data.
     *
     * @param appendableBytes The wrapped byte array into which the persistent data is to be serialized.
     */
    @Override
    protected void serialize(AppendableBytes appendableBytes) {
        appendableBytes.writeLong(value);
    }

    @Override
    public void initialize(final Mailbox mailbox, Ancestor parent, FactoryImpl factory)
            throws Exception {
        super.initialize(mailbox, parent, factory);
        getLongReq = new BoundRequestBase<Long>(getMailbox()) {
            @Override
            public void processRequest(Transport rp) throws Exception {
                rp.processResponse(getValue());
            }
        };
    }
}
