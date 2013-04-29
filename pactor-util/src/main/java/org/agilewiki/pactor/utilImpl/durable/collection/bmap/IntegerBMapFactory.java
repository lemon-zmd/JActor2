package org.agilewiki.pactor.utilImpl.durable.collection.bmap;

import org.agilewiki.pactor.api.Mailbox;
import org.agilewiki.pactor.util.Ancestor;
import org.agilewiki.pactor.util.durable.*;
import org.agilewiki.pactor.utilImpl.durable.FactoryImpl;
import org.agilewiki.pactor.utilImpl.durable.collection.smap.IntegerSMapFactory;
import org.agilewiki.pactor.utilImpl.durable.scalar.vlens.UnionImpl;

/**
 * Creates IntegerBMap's.
 */
public class IntegerBMapFactory extends FactoryImpl {

    private final static int NODE_CAPACITY = 28;

    public static void registerFactories(final FactoryLocator _factoryLocator) {
        registerFactory(_factoryLocator, PAMap.INTEGER_PASTRING_BMAP, PAString.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_BYTES_BMAP, Bytes.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_BOX_BMAP, Box.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_PALONG_BMAP, PALong.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_PAINTEGER_BMAP, PAInteger.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_PAFLOAT_BMAP, PAFloat.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_PADOUBLE_BMAP, PADouble.FACTORY_NAME);
        registerFactory(_factoryLocator, PAMap.INTEGER_PABOOLEAN_BMAP, PABoolean.FACTORY_NAME);
    }

    public static void registerFactory(FactoryLocator factoryLocator,
                                       String actorType,
                                       String valueType) {
        UnionImpl.registerFactory(factoryLocator,
                "U." + actorType, "LM." + actorType, "IM." + actorType);

        factoryLocator.registerFactory(new IntegerBMapFactory(
                actorType, valueType, true, true));
        factoryLocator.registerFactory(new IntegerBMapFactory(
                "IN." + actorType, valueType, false, false));

        IntegerSMapFactory.registerFactory(
                factoryLocator, "LM." + actorType, valueType, NODE_CAPACITY);
        IntegerSMapFactory.registerFactory(
                factoryLocator, "IM." + actorType, "IN." + actorType, NODE_CAPACITY);
    }

    private String valueType;
    private boolean isRoot = true;
    private boolean auto = true;

    /**
     * Create an FactoryImpl.
     *
     * @param jidType   The jid type.
     * @param valueType The value type.
     */
    protected IntegerBMapFactory(String jidType, String valueType,
                                 boolean isRoot, boolean auto) {
        super(jidType);
        this.valueType = valueType;
        this.isRoot = isRoot;
        this.auto = auto;
    }

    /**
     * Create a JLPCActor.
     *
     * @return The new actor.
     */
    @Override
    protected IntegerBMap instantiateActor() {
        return new IntegerBMap();
    }

    /**
     * Create and configure an actor.
     *
     * @param mailbox The mailbox of the new actor.
     * @param parent  The parent of the new actor.
     * @return The new actor.
     */
    @Override
    public IntegerBMap newSerializable(Mailbox mailbox, Ancestor parent) {
        IntegerBMap imj = (IntegerBMap) super.newSerializable(mailbox, parent);
        FactoryLocator fl = Durables.getFactoryLocator(mailbox);
        imj.valueFactory = fl.getFactory(valueType);
        imj.nodeCapacity = NODE_CAPACITY;
        imj.isRoot = isRoot;
        imj.init();
        if (auto)
            imj.setNodeLeaf();
        return imj;
    }
}