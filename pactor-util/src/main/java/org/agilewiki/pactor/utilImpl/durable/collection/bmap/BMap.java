package org.agilewiki.pactor.utilImpl.durable.collection.bmap;

import org.agilewiki.pactor.api.Mailbox;
import org.agilewiki.pactor.api.Request;
import org.agilewiki.pactor.api.RequestBase;
import org.agilewiki.pactor.api.Transport;
import org.agilewiki.pactor.util.Ancestor;
import org.agilewiki.pactor.util.durable.*;
import org.agilewiki.pactor.utilImpl.durable.DurableImpl;
import org.agilewiki.pactor.utilImpl.durable.FactoryImpl;
import org.agilewiki.pactor.utilImpl.durable.collection.MapEntryImpl;
import org.agilewiki.pactor.utilImpl.durable.collection.smap.SMap;
import org.agilewiki.pactor.utilImpl.durable.scalar.flens.PAIntegerImpl;
import org.agilewiki.pactor.utilImpl.durable.scalar.vlens.UnionImpl;

/**
 * A balanced tree that holds a map.
 */
abstract public class BMap<KEY_TYPE extends Comparable<KEY_TYPE>, VALUE_TYPE extends PASerializable>
        extends DurableImpl
        implements PAMap<KEY_TYPE, VALUE_TYPE>, Collection<MapEntry<KEY_TYPE, VALUE_TYPE>> {
    protected final int TUPLE_SIZE = 0;
    protected final int TUPLE_UNION = 1;
    protected int nodeCapacity = 28;
    protected boolean isRoot;
    public Factory valueFactory;
    protected FactoryLocator factoryLocator;

    private Request<Integer> sizeReq;
    private Request<Void> emptyReq;
    private Request<MapEntry<KEY_TYPE, VALUE_TYPE>> getFirstReq;
    private Request<MapEntry<KEY_TYPE, VALUE_TYPE>> getLastReq;

    @Override
    public Request<Integer> sizeReq() {
        return sizeReq;
    }

    public Request<Void> emptyReq() {
        return emptyReq;
    }

    public Request<MapEntry<KEY_TYPE, VALUE_TYPE>> getFirstReq() {
        return getFirstReq;
    }

    public Request<MapEntry<KEY_TYPE, VALUE_TYPE>> getLastReq() {
        return getLastReq;
    }

    /**
     * Converts a string to a key.
     *
     * @param skey The string to be converted.
     * @return The key.
     */
    abstract protected KEY_TYPE stringToKey(String skey);

    protected Factory getValueFactory() {
        if (valueFactory == null)
            throw new IllegalStateException("valueFactory uninitialized");
        return valueFactory;
    }

    protected void init() {
        String baseType = getType();
        if (baseType.startsWith("IN."))
            baseType = baseType.substring(3);
        factoryLocator = Durables.getFactoryLocator(getMailbox());
        tupleFactories = new FactoryImpl[2];
        tupleFactories[TUPLE_SIZE] = factoryLocator.getFactory(PAInteger.FACTORY_NAME);
        tupleFactories[TUPLE_UNION] = factoryLocator.getFactory("U." + baseType);
    }

    protected void setNodeLeaf() {
        getUnionJid().setValue(0);
    }

    protected void setNodeFactory(FactoryImpl factoryImpl) {
        getUnionJid().setValue(factoryImpl);
    }

    protected PAIntegerImpl getSizeJid() {
        return (PAIntegerImpl) _iGet(TUPLE_SIZE);
    }

    /**
     * Returns the size of the collection.
     *
     * @return The size of the collection.
     */
    @Override
    public int size() {
        return getSizeJid().getValue();
    }

    protected void incSize(int inc) {
        PAIntegerImpl sj = getSizeJid();
        sj.setValue(sj.getValue() + inc);
    }

    protected UnionImpl getUnionJid() {
        return (UnionImpl) _iGet(TUPLE_UNION);
    }

    protected SMap<KEY_TYPE, PASerializable> getNode() {
        return (SMap) getUnionJid().getValue();
    }

    public String getNodeFactoryKey() {
        return getNode().getFactory().getFactoryKey();
    }

    public boolean isLeaf() {
        return getNodeFactoryKey().startsWith("LM.");
    }

    public int nodeSize() {
        return getNode().size();
    }

    public boolean isFat() {
        return nodeSize() >= nodeCapacity;
    }

    @Override
    public Request<MapEntry<KEY_TYPE, VALUE_TYPE>> iGetReq(final int _i) {
        return new RequestBase<MapEntry<KEY_TYPE, VALUE_TYPE>>(getMailbox()) {
            @Override
            public void processRequest(Transport<MapEntry<KEY_TYPE, VALUE_TYPE>> _rp) throws Exception {
                _rp.processResponse(iGet(_i));
            }
        };
    }

    /**
     * Returns the selected element.
     *
     * @param ndx Selects the element.
     * @return The ith JID component, or null if the index is out of range.
     */
    @Override
    public MapEntryImpl<KEY_TYPE, VALUE_TYPE> iGet(int ndx) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        if (isLeaf()) {
            return (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(ndx);
        }
        if (ndx < 0)
            ndx += size();
        if (ndx < 0 || ndx >= size())
            return null;
        int i = 0;
        while (i < node.size()) {
            BMap<KEY_TYPE, VALUE_TYPE> bnode = (BMap) ((MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(i)).getValue();
            int bns = bnode.size();
            if (ndx < bns) {
                return bnode.iGet(ndx);
            }
            ndx -= bns;
            i += 1;
        }
        return null;
    }

    @Override
    public Request<Void> iSetReq(final int _i, final byte[] _bytes) {
        return new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport _rp) throws Exception {
                iSet(_i, _bytes);
                _rp.processResponse(null);
            }
        };
    }

    /**
     * Creates a JID actor and loads its serialized data.
     *
     * @param ndx   The index of the desired element.
     * @param bytes Holds the serialized data.
     */
    @Override
    public void iSet(int ndx, byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Request<Void> iAddReq(final int _i) {
        return new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport<Void> _rp) throws Exception {
                iAdd(_i);
                _rp.processResponse(null);
            }
        };
    }

    @Override
    public void iAdd(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Request<Void> iAddReq(final int _i, final byte[] _bytes) {
        return new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport<Void> _rp) throws Exception {
                iAdd(_i, _bytes);
                _rp.processResponse(null);
            }
        };
    }

    @Override
    public void iAdd(int ndx, byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Request<Boolean> kMakeReq(final KEY_TYPE _key, final byte[] _bytes) {
        return new RequestBase<Boolean>(getMailbox()) {
            @Override
            public void processRequest(Transport<Boolean> _rp) throws Exception {
                _rp.processResponse(kMake(_key, _bytes));
            }
        };
    }

    /**
     * Add a tuple to the map unless there is a tuple with a matching first element.
     *
     * @param key   Used to match the first element of the tuples.
     * @param bytes The serialized form of a JID of the appropriate type.
     * @return True if a new tuple was created; otherwise the old value is unaltered.
     */
    public Boolean kMake(KEY_TYPE key, byte[] bytes) {
        if (!kMake(key))
            return false;
        kSet(key, bytes);
        return true;
    }

    @Override
    public Request<Boolean> kMakeReq(final KEY_TYPE _key) {
        return new RequestBase<Boolean>(getMailbox()) {
            @Override
            public void processRequest(Transport<Boolean> _rp) throws Exception {
                _rp.processResponse(kMake(_key));
            }
        };
    }

    /**
     * Add an entry to the map unless there is an entry with a matching first element.
     *
     * @param key Used to match the first element of the entries.
     * @return True if a new entry was created.
     */
    @Override
    final public Boolean kMake(KEY_TYPE key) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        if (isLeaf()) {
            int i = node.search(key);
            if (i > -1)
                return false;
            i = -i - 1;
            node.iAdd(i);
            MapEntryImpl<KEY_TYPE, VALUE_TYPE> me = (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(i);
            me.setKey(key);
            incSize(1);
            return true;
        }
        int i = node.match(key);
        MapEntryImpl<KEY_TYPE, VALUE_TYPE> entry = null;
        if (node.size() == i) {
            i -= 1;
            entry = (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(i);
            entry.setKey(key);
        } else {
            entry = (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(i);
        }
        BMap<KEY_TYPE, VALUE_TYPE> bnode = (BMap) entry.getValue();
        if (!bnode.kMake(key))
            return false;
        incSize(1);
        if (bnode.isFat()) {
            node.iAdd(i - 1);
            MapEntryImpl<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> leftEntry = (MapEntryImpl) node.iGet(i - 1);
            bnode.inodeSplit(leftEntry);
            if (node.size() < nodeCapacity)
                return true;
            if (isRoot) {
                rootSplit();
            }
        }
        return true;
    }

    protected void rootSplit() {
        SMap<KEY_TYPE, PASerializable> oldRootNode = getNode();
        FactoryImpl oldFactory = oldRootNode.getFactory();
        getUnionJid().setValue(1);
        SMap<KEY_TYPE, PASerializable> newRootNode = getNode();
        newRootNode.iAdd(0);
        newRootNode.iAdd(1);
        MapEntryImpl<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> leftEntry = (MapEntryImpl) newRootNode.iGet(0);
        MapEntryImpl<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> rightEntry = (MapEntryImpl) newRootNode.iGet(1);
        BMap<KEY_TYPE, PASerializable> leftBNode = leftEntry.getValue();
        BMap<KEY_TYPE, PASerializable> rightBNode = rightEntry.getValue();
        leftBNode.setNodeFactory(oldFactory);
        rightBNode.setNodeFactory(oldFactory);
        int h = nodeCapacity / 2;
        int i = 0;
        if (oldFactory.name.startsWith("LM.")) {
            while (i < h) {
                PASerializable e = oldRootNode.iGet(i);
                byte[] bytes = e.getDurable().getSerializedBytes();
                leftBNode.iAdd(-1, bytes);
                i += 1;
            }
            while (i < nodeCapacity) {
                PASerializable e = oldRootNode.iGet(i);
                byte[] bytes = e.getDurable().getSerializedBytes();
                rightBNode.iAdd(-1, bytes);
                i += 1;
            }
        } else {
            while (i < h) {
                BMap<KEY_TYPE, PASerializable> e =
                        (BMap) ((MapEntryImpl<KEY_TYPE, VALUE_TYPE>) oldRootNode.iGet(i)).getValue();
                int eSize = e.size();
                byte[] bytes = e.getSerializedBytes();
                leftBNode.append(bytes, eSize);
                i += 1;
            }
            while (i < nodeCapacity) {
                BMap<KEY_TYPE, PASerializable> e =
                        (BMap) ((MapEntryImpl<KEY_TYPE, VALUE_TYPE>) oldRootNode.iGet(i)).getValue();
                int eSize = e.size();
                byte[] bytes = e.getSerializedBytes();
                rightBNode.append(bytes, eSize);
                i += 1;
            }
        }
        leftEntry.setKey(leftBNode.getLastKey());
        rightEntry.setKey(rightBNode.getLastKey());
    }

    protected void inodeSplit(MapEntryImpl<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> leftEntry) {
        BMap<KEY_TYPE, PASerializable> leftBNode = leftEntry.getValue();
        leftBNode.setNodeFactory(getNode().getFactory());
        SMap<KEY_TYPE, PASerializable> node = getNode();
        int h = nodeCapacity / 2;
        int i = 0;
        if (isLeaf()) {
            while (i < h) {
                PASerializable e = node.iGet(0);
                node.iRemove(0);
                byte[] bytes = e.getDurable().getSerializedBytes();
                leftBNode.iAdd(-1, bytes);
                i += 1;
            }
            incSize(-h);
        } else {
            while (i < h) {
                BMap<KEY_TYPE, VALUE_TYPE> e = (BMap) ((MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(0)).getValue();
                node.iRemove(0);
                int eSize = e.size();
                incSize(-eSize);
                byte[] bytes = e.getSerializedBytes();
                leftBNode.append(bytes, eSize);
                i += 1;
            }
        }
        KEY_TYPE leftKey = leftBNode.getLastKey();
        leftEntry.setKey(leftKey);
    }

    @Override
    public void empty() {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        node.empty();
        PAIntegerImpl sj = getSizeJid();
        sj.setValue(0);
    }

    @Override
    public Request<Void> iRemoveReq(final int _i) {
        return new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport<Void> _rp) throws Exception {
                iRemove(_i);
                _rp.processResponse(null);
            }
        };
    }

    @Override
    public void iRemove(int ndx) {
        int s = size();
        if (ndx < 0)
            ndx += s;
        if (ndx < 0 || ndx >= s)
            throw new IllegalArgumentException();
        SMap<KEY_TYPE, PASerializable> node = getNode();
        if (isLeaf()) {
            node.iRemove(ndx);
            incSize(-1);
            return;
        }
        int i = 0;
        while (i < node.size()) {
            MapEntryImpl<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> entry = (MapEntryImpl) node.iGet(ndx);
            BMap<KEY_TYPE, VALUE_TYPE> bnode = (BMap) entry.getValue();
            int bns = bnode.size();
            if (ndx < bns) {
                bnode.iRemove(ndx);
                incSize(-1);
                int bnodeSize = bnode.size();
                if (bnodeSize > nodeCapacity / 3) {
                    entry.setKey(bnode.getLastKey());
                    return;
                }
                if (bnodeSize == 0) {
                    node.iRemove(ndx);
                } else {
                    entry.setKey(bnode.getLastKey());
                    if (i > 0) {
                        MapEntryImpl leftEntry = (MapEntryImpl) node.iGet(i - 1);
                        BMap<KEY_TYPE, VALUE_TYPE> leftBNode = (BMap) leftEntry.getValue();
                        if (leftBNode.nodeSize() + bnodeSize < nodeCapacity) {
                            bnode.appendTo(leftBNode);
                            node.iRemove(i);
                            leftEntry.setKey(leftBNode.getLastKey());
                        }
                    }
                    if (i + 1 < node.size()) {
                        MapEntryImpl rightEntry = (MapEntryImpl) node.iGet(i + 1);
                        BMap<KEY_TYPE, VALUE_TYPE> rightBNode = (BMap) rightEntry.getValue();
                        if (bnodeSize + rightBNode.nodeSize() < nodeCapacity) {
                            rightBNode.appendTo(bnode);
                            node.iRemove(i + 1);
                            rightEntry.setKey(rightBNode.getLastKey());
                        }
                    }
                }
                if (node.size() == 1 && isRoot && !isLeaf()) {
                    bnode = (BMap) ((MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.iGet(0)).getValue();
                    setNodeFactory(bnode.getNode().getFactory());
                    PAIntegerImpl sj = getSizeJid();
                    sj.setValue(0);
                    bnode.appendTo(this);
                }
                return;
            }
            ndx -= bns;
            i += 1;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Request<Boolean> kRemoveReq(final KEY_TYPE _key) {
        return new RequestBase<Boolean>(getMailbox()) {
            @Override
            public void processRequest(Transport<Boolean> _rp) throws Exception {
                _rp.processResponse(kRemove(_key));
            }
        };
    }

    /**
     * Removes the item identified by the key.
     *
     * @param key The key.
     * @return True when the item was present and removed.
     */
    @Override
    final public boolean kRemove(KEY_TYPE key) {
        if (isLeaf()) {
            SMap<KEY_TYPE, PASerializable> node = getNode();
            if (node.kRemove(key)) {
                incSize(-1);
                return true;
            }
            return false;
        }
        SMap<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> node = (SMap) getNode();
        int i = node.match(key);
        if (i == size())
            return false;
        MapEntryImpl<KEY_TYPE, BMap<KEY_TYPE, PASerializable>> entry = (MapEntryImpl) node.iGet(i);
        BMap<KEY_TYPE, PASerializable> bnode = entry.getValue();
        if (!bnode.kRemove(key))
            return false;
        incSize(-1);
        int bnodeSize = bnode.size();
        if (bnodeSize > nodeCapacity / 3)
            return true;
        if (bnodeSize == 0) {
            node.iRemove(i);
        } else {
            entry.setKey(bnode.getLastKey());
            if (i > 0) {
                MapEntryImpl leftEntry = (MapEntryImpl) node.iGet(i - 1);
                BMap<KEY_TYPE, VALUE_TYPE> leftBNode = (BMap) leftEntry.getValue();
                if (leftBNode.nodeSize() + bnodeSize < nodeCapacity) {
                    bnode.appendTo((BMap<KEY_TYPE, PASerializable>) leftBNode);
                    node.iRemove(i);
                    leftEntry.setKey(leftBNode.getLastKey());
                }
            }
            if (i + 1 < node.size()) {
                MapEntryImpl rightEntry = (MapEntryImpl) node.iGet(i + 1);
                BMap<KEY_TYPE, VALUE_TYPE> rightBNode = (BMap) rightEntry.getValue();
                if (bnodeSize + rightBNode.nodeSize() < nodeCapacity) {
                    rightBNode.appendTo((BMap<KEY_TYPE, VALUE_TYPE>) bnode);
                    node.iRemove(i + 1);
                    rightEntry.setKey(rightBNode.getLastKey());
                }
            }
        }
        if (node.size() == 1 && isRoot && !isLeaf()) {
            bnode = (BMap) ((MapEntryImpl) node.iGet(0)).getValue();
            setNodeFactory(bnode.getNode().getFactory());
            PAIntegerImpl sj = getSizeJid();
            sj.setValue(0);
            bnode.appendTo((BMap<KEY_TYPE, PASerializable>) this);
        }
        return true;
    }

    void appendTo(BMap<KEY_TYPE, VALUE_TYPE> leftNode) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        int i = 0;
        if (isLeaf()) {
            while (i < node.size()) {
                PASerializable e = node.iGet(i);
                leftNode.append(e.getDurable().getSerializedBytes(), 1);
                i += 1;
            }
        } else {
            while (i < node.size()) {
                BMap<KEY_TYPE, VALUE_TYPE> e = (BMap) ((MapEntryImpl) node.iGet(i)).getValue();
                leftNode.append(e.getSerializedBytes(), e.size());
                i += 1;
            }
        }
    }

    void append(byte[] bytes, int eSize) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        node.iAdd(-1, bytes);
        incSize(eSize);
    }

    final public MapEntryImpl<KEY_TYPE, VALUE_TYPE> kGetEntry(KEY_TYPE key) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        if (isLeaf()) {
            int i = node.search(key);
            if (i < 0)
                return null;
            return iGet(i);
        }
        int i = node.match(key);
        if (i == size())
            return null;
        BMap<KEY_TYPE, VALUE_TYPE> bnode = (BMap) ((MapEntryImpl) node.iGet(i)).getValue();
        return bnode.kGetEntry(key);
    }

    @Override
    public Request<VALUE_TYPE> kGetReq(final KEY_TYPE _key) {
        return new RequestBase<VALUE_TYPE>(getMailbox()) {
            @Override
            public void processRequest(Transport<VALUE_TYPE> _rp) throws Exception {
                _rp.processResponse(kGet(_key));
            }
        };
    }

    /**
     * Returns the JID value associated with the key.
     *
     * @param key The key.
     * @return The jid assigned to the key, or null.
     */
    @Override
    final public VALUE_TYPE kGet(KEY_TYPE key) {
        MapEntryImpl<KEY_TYPE, VALUE_TYPE> entry = kGetEntry(key);
        if (entry == null)
            return null;
        return entry.getValue();
    }

    @Override
    public Request<MapEntry<KEY_TYPE, VALUE_TYPE>> getCeilingReq(final KEY_TYPE _key) {
        return new RequestBase<MapEntry<KEY_TYPE, VALUE_TYPE>>(getMailbox()) {
            @Override
            public void processRequest(Transport<MapEntry<KEY_TYPE, VALUE_TYPE>> _rp) throws Exception {
                _rp.processResponse(getCeiling(_key));
            }
        };
    }

    /**
     * Returns the JID value with the smallest key >= the given key.
     *
     * @param key The key.
     * @return The matching jid, or null.
     */
    @Override
    final public MapEntryImpl<KEY_TYPE, VALUE_TYPE> getCeiling(KEY_TYPE key) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        if (isLeaf()) {
            return (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.getCeiling(key);
        }
        int i = node.match(key);
        if (i == size())
            return null;
        BMap<KEY_TYPE, VALUE_TYPE> bnode = (BMap) ((MapEntryImpl) node.iGet(i)).getValue();
        return bnode.getCeiling(key);
    }

    @Override
    public Request<MapEntry<KEY_TYPE, VALUE_TYPE>> getHigherReq(final KEY_TYPE _key) {
        return new RequestBase<MapEntry<KEY_TYPE, VALUE_TYPE>>(getMailbox()) {
            @Override
            public void processRequest(Transport<MapEntry<KEY_TYPE, VALUE_TYPE>> _rp) throws Exception {
                _rp.processResponse(getHigher(_key));
            }
        };
    }

    /**
     * Returns the JID value with a greater key.
     *
     * @param key The key.
     * @return The matching jid, or null.
     */
    @Override
    final public MapEntryImpl<KEY_TYPE, VALUE_TYPE> getHigher(KEY_TYPE key) {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        MapEntryImpl entry = node.getHigher(key);
        if (isLeaf())
            return (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) entry;
        if (entry == null)
            return null;
        BMap<KEY_TYPE, VALUE_TYPE> bnode = (BMap) entry.getValue();
        return bnode.getHigher(key);
    }

    /**
     * Resolves a JID pathname, returning a JID actor or null.
     *
     * @param pathname A JID pathname.
     * @return A JID actor or null.
     * @throws Exception Any uncaught exception which occurred while processing the request.
     */
    @Override
    final public PASerializable resolvePathname(String pathname) {
        if (pathname.length() == 0) {
            throw new IllegalArgumentException("empty string");
        }
        int s = pathname.indexOf("/");
        if (s == -1)
            s = pathname.length();
        if (s == 0)
            throw new IllegalArgumentException("pathname " + pathname);
        String ns = pathname.substring(0, s);
        PASerializable jid = kGet(stringToKey(ns));
        if (jid == null)
            return null;
        if (s == pathname.length())
            return jid;
        return jid.getDurable().resolvePathname(pathname.substring(s + 1));
    }

    public MapEntryImpl<KEY_TYPE, VALUE_TYPE> getFirst() {
        return iGet(0);
    }

    public MapEntryImpl<KEY_TYPE, VALUE_TYPE> getLast() {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        return (MapEntryImpl<KEY_TYPE, VALUE_TYPE>) node.getLast();
    }

    public KEY_TYPE getLastKey() {
        SMap<KEY_TYPE, PASerializable> node = getNode();
        return node.getLastKey();
    }

    @Override
    public Request<Void> kSetReq(final KEY_TYPE _key, final byte[] _bytes) {
        return new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport<Void> _rp) throws Exception {
                kSet(_key, _bytes);
                _rp.processResponse(null);
            }
        };
    }

    @Override
    public void kSet(KEY_TYPE key, byte[] bytes) {
        MapEntryImpl<KEY_TYPE, VALUE_TYPE> entry = kGetEntry(key);
        if (entry == null)
            throw new IllegalArgumentException("not present: " + key);
        entry.setValueBytes(bytes);
    }

    public void initialize(final Mailbox mailbox, Ancestor parent, FactoryImpl factory) {
        super.initialize(mailbox, parent, factory);
        sizeReq = new RequestBase<Integer>(getMailbox()) {
            @Override
            public void processRequest(Transport<Integer> _rp) throws Exception {
                _rp.processResponse(size());
            }
        };
        emptyReq = new RequestBase<Void>(getMailbox()) {
            @Override
            public void processRequest(Transport<Void> _rp) throws Exception {
                empty();
                _rp.processResponse(null);
            }
        };
        getFirstReq = new RequestBase<MapEntry<KEY_TYPE, VALUE_TYPE>>(getMailbox()) {
            @Override
            public void processRequest(Transport<MapEntry<KEY_TYPE, VALUE_TYPE>> _rp) throws Exception {
                _rp.processResponse(getFirst());
            }
        };
        getLastReq = new RequestBase<MapEntry<KEY_TYPE, VALUE_TYPE>>(getMailbox()) {
            @Override
            public void processRequest(Transport<MapEntry<KEY_TYPE, VALUE_TYPE>> _rp) throws Exception {
                _rp.processResponse(getLast());
            }
        };
    }
}