package org.agilewiki.jactor.util.durable.block;

import junit.framework.TestCase;
import org.agilewiki.jactor.api.MailboxFactory;
import org.agilewiki.jactor.util.durable.Durables;
import org.agilewiki.jactor.util.durable.FactoryLocator;
import org.agilewiki.jactor.util.durable.incDes.Root;

public class LBlockTest extends TestCase {
    public void test()
            throws Exception {
        MailboxFactory mailboxFactory = Durables.createMailboxFactory();
        try {
            FactoryLocator factoryLocator = Durables.getFactoryLocator(mailboxFactory);
            Root rj = (Root) Durables.newSerializable(
                    factoryLocator,
                    Root.FACTORY_NAME,
                    mailboxFactory.createMailbox());
            LBlock lb1 = new LBlock();
            lb1.setRootJid(rj);
            byte[] bs = lb1.serialize();

            int hl = lb1.headerLength();
            int rjl = rj.getSerializedLength();
            assertEquals(bs.length, hl + rjl);

            byte[] h = new byte[hl];
            System.arraycopy(bs, 0, h, 0, hl);
            byte[] sd = new byte[rjl];
            System.arraycopy(bs, hl, sd, 0, rjl);

            LBlock lb2 = new LBlock();
            int rjl2 = lb2.setHeaderBytes(h);
            lb2.setRootJidBytes(sd);
            Root rj2 = lb2.getRootJid(factoryLocator, mailboxFactory.createMailbox(), null);
        } finally {
            mailboxFactory.close();
        }
    }
}
