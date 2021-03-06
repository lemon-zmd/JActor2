package org.agilewiki.jactor2.util.durable.app;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.agilewiki.jactor2.core.facilities.Plant;
import org.agilewiki.jactor2.util.durable.Durables;
import org.agilewiki.jactor2.util.durable.FactoryLocator;
import org.agilewiki.jactor2.util.durable.incDes.Box;

public class AppTest extends TestCase {
    public void test1() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            FactoryLocator factoryLocator = Durables.getFactoryLocator(plant);
            User.register(factoryLocator);
            User user1 = (User) Durables.newSerializable(plant, "user");
            user1.PAName().setValue("Joe");
            user1.PAAge().setValue(42);
            user1.PALocation().setValue("Boston");

            User user2 = (User) user1.getDurable().copy(null);
            Assert.assertEquals("Joe", user2.PAName().getValue());
            assertEquals(42, (int) user2.PAAge().getValue());
            Assert.assertEquals("Boston", user2.PALocation().getValue());
        } finally {
            plant.close();
        }
    }

    public void test2() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            FactoryLocator factoryLocator = Durables.getFactoryLocator(plant);
            User.register(factoryLocator);
            Box box1 = (Box) Durables.newSerializable(plant, Box.FACTORY_NAME);
            box1.setValue("user");
            User user1 = (User) box1.getValue();
            user1.PAName().setValue("Joe");
            user1.PAAge().setValue(42);
            user1.PALocation().setValue("Boston");

            Box box2 = (Box) box1.copy(null);
            User user2 = (User) box2.getValue();
            Assert.assertEquals("Joe", user2.PAName().getValue());
            assertEquals(42, (int) user2.PAAge().getValue());
            Assert.assertEquals("Boston", user2.PALocation().getValue());
        } finally {
            plant.close();
        }
    }
}
