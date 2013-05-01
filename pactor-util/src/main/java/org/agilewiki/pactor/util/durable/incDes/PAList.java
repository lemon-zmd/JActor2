package org.agilewiki.pactor.util.durable.incDes;

import org.agilewiki.pactor.api.Request;
import org.agilewiki.pactor.util.durable.PASerializable;

public interface PAList<ENTRY_TYPE extends PASerializable> extends Collection<ENTRY_TYPE> {

    public final static String PASTRING_LIST = "stringList";
    public final static String BYTES_LIST = "bytesList";
    public final static String BOX_LIST = "boxList";
    public final static String PALONG_LIST = "longList";
    public final static String PAINTEGER_LIST = "intList";
    public final static String PAFLOAT_LIST = "floatList";
    public final static String PADOUBLE_LIST = "doubleList";
    public final static String PABOOLEAN_LIST = "boolList";

    Request<Void> emptyReq();

    void empty();

    Request<Void> iAddReq(final int _i);

    void iAdd(final int _i);

    Request<Void> iAddReq(final int _i, final byte[] _bytes);

    void iAdd(final int _i, final byte[] _bytes);

    Request<Void> iRemoveReq(final int _i);

    void iRemove(final int _i);
}