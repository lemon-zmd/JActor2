package org.agilewiki.jactor2.util.durable.incDes;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.facilities.Plant;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.agilewiki.jactor2.util.durable.Durables;

public class BListTest extends TestCase {
    public void test1() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            JAList<JAString> stringList1 = (JAList) Durables.newSerializable(plant, JAList.JASTRING_LIST);
            stringList1.iAdd(0);
            stringList1.iAdd(1);
            stringList1.iAdd(2);
            JAString sj0 = stringList1.iGet(0);
            JAString sj1 = stringList1.iGet(1);
            JAString sj2 = stringList1.iGet(2);
            sj0.setValue("a");
            sj1.setValue("b");
            sj2.setValue("c");
            Reactor reactor = new NonBlockingReactor(plant);
            JAList<JAString> stringList2 = (JAList) stringList1.copy(reactor);
            JAString s0 = stringList2.iGet(0);
            JAString s1 = stringList2.iGet(1);
            JAString s2 = stringList2.iGet(2);
            assertEquals("a", s0.getValue());
            assertEquals("b", s1.getValue());
            assertEquals("c", s2.getValue());
        } finally {
            plant.close();
        }
    }

    public void test2() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            JAList<JAInteger> intList1 = (JAList) Durables.newSerializable(plant, JAList.JAINTEGER_LIST);
            int i = 0;
            while (i < 28) {
                intList1.iAdd(i);
                JAInteger ij0 = intList1.iGet(i);
                ij0.setValue(i);
                i += 1;
            }
            i = 0;
            while (i < 28) {
                JAInteger ij = intList1.iGet(i);
                assertEquals(i, (int) ij.getValue());
                i += 1;
            }
        } finally {
            plant.close();
        }
    }

    public void test3() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            JAList<JAInteger> intList1 = (JAList) Durables.newSerializable(plant, JAList.JAINTEGER_LIST);
            int i = 0;
            while (i < 41) {
                intList1.iAdd(-1);
                JAInteger ij0 = intList1.iGet(-1);
                ij0.setValue(i);
                i += 1;
            }
            i = 0;
            while (i < 41) {
                JAInteger ij = intList1.iGet(i);
                assertEquals(i, (int) ij.getValue());
                i += 1;
            }
        } finally {
            plant.close();
        }
    }

    public void test4() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            JAList<JAInteger> intList1 = (JAList) Durables.newSerializable(plant, JAList.JAINTEGER_LIST);
            int i = 0;
            while (i < 391) {
                intList1.iAdd(-1);
                JAInteger ij0 = intList1.iGet(-1);
                ij0.setValue(i);
                i += 1;
            }
            i = 0;
            while (i < 391) {
                JAInteger ij = intList1.iGet(i);
                assertEquals(i, (int) ij.getValue());
                i += 1;
            }
        } finally {
            plant.close();
        }
    }

    public void test5() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            JAList<JAInteger> intList1 = (JAList) Durables.newSerializable(plant, JAList.JAINTEGER_LIST);
            int i = 0;
            while (i < 10000) {
                intList1.iAdd(-1);
                JAInteger ij0 = intList1.iGet(-1);
                ij0.setValue(i);
                i += 1;
            }
            i = 0;
            while (i < 10000) {
                JAInteger ij = intList1.iGet(i);
                assertEquals(i, (int) ij.getValue());
                i += 1;
            }
        } finally {
            plant.close();
        }
    }

    public void test6() throws Exception {
        Plant plant = Durables.createFacility();
        try {
            JAList<JAInteger> intList1 = (JAList) Durables.newSerializable(plant, JAList.JAINTEGER_LIST);
            int i = 0;
            while (i < 10000) {
                intList1.iAdd(-1);
                JAInteger ij0 = intList1.iGet(-1);
                ij0.setValue(i);
                i += 1;
            }
            i = 0;
            while (i < 10000) {
                intList1.iRemove(-1);
                i += 1;
            }
            assertEquals(0, intList1.size());
        } finally {
            plant.close();
        }
    }
}
