package org.agilewiki.pactor.durable.impl.scalar.flens;

import org.agilewiki.pactor.Mailbox;
import org.agilewiki.pactor.Request;
import org.agilewiki.pactor.RequestBase;
import org.agilewiki.pactor.Transport;
import org.agilewiki.pactor.durable.*;
import org.agilewiki.pactor.durable.impl.FactoryImpl;
import org.agilewiki.pautil.Ancestor;

/**
 * A JID actor that holds a double.
 */
public class PADoubleImpl
        extends FLenScalar<Double> implements PADouble {

    public static void registerFactory(FactoryLocator factoryLocator)
            throws Exception {
        factoryLocator.registerFactory(new FactoryImpl(PADouble.FACTORY_NAME) {
            @Override
            final protected PADoubleImpl instantiateActor() {
                return new PADoubleImpl();
            }
        });
    }

    private Request<Double> getDoubleReq;

    @Override
    public Request<Double> getDoubleReq() {
        return getDoubleReq;
    }

    /**
     * Create the value.
     *
     * @return The default value
     */
    @Override
    protected Double newValue() {
        return new Double(0.D);
    }

    /**
     * Returns the value held by this component.
     *
     * @return The value held by this component.
     */
    @Override
    public Double getValue() {
        if (value != null)
            return value;
        ReadableBytes readableBytes = readable();
        value = readableBytes.readDouble();
        return value;
    }

    @Override
    public Request<Void> setDoubleReq(final Double v) {
        return new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport rp) throws Exception {
                setValue(v);
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
        return Util.DOUBLE_LENGTH;
    }

    /**
     * Serialize the persistent data.
     *
     * @param appendableBytes The wrapped byte array into which the persistent data is to be serialized.
     */
    @Override
    protected void serialize(AppendableBytes appendableBytes) {
        appendableBytes.writeDouble(value);
    }

    @Override
    public void initialize(final Mailbox mailbox, Ancestor parent, FactoryImpl factory) {
        super.initialize(mailbox, parent, factory);
        getDoubleReq = new RequestBase<Double>(getMailbox()) {
            @Override
            public void processRequest(Transport rp) throws Exception {
                rp.processResponse(getValue());
            }
        };
    }
}