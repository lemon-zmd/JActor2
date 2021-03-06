package org.agilewiki.jactor2.core.messages;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.facilities.Facility;
import org.agilewiki.jactor2.core.facilities.Plant;
import org.agilewiki.jactor2.core.reactors.IsolationReactor;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;
import org.agilewiki.jactor2.core.reactors.Reactor;

/**
 * Test code.
 */
public class Test4 extends TestCase {
    public void testb() throws Exception {
        final Plant plant = new Plant();
        final Reactor reactor = new NonBlockingReactor(plant);
        new Blade4(reactor).hi4SReq().call();
        plant.close();
    }

    public void testd() throws Exception {
        final Facility facility = new Facility();
        new Blade4(new IsolationReactor(facility)).hi4SReq().call();
        facility.close();
    }
}
