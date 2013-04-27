package org.agilewiki.general.messaging;

import junit.framework.TestCase;
import org.agilewiki.pactor.api.Mailbox;
import org.agilewiki.pactor.api.MailboxFactory;
import org.agilewiki.pactor.api.ResponseProcessor;
import org.agilewiki.pactor.impl.DefaultMailboxFactoryImpl;

/**
 * Test code.
 */
public class ThreadBoundTest extends TestCase {
    Mailbox boundMailbox;
    MailboxFactory mailboxFactory;

    public void testa() throws Exception {
        mailboxFactory = new DefaultMailboxFactoryImpl();
        boundMailbox = mailboxFactory.createThreadBoundMailbox(new Runnable() {
            @Override
            public void run() {
                boundMailbox.run();
                try {
                    mailboxFactory.close();
                } catch (final Throwable x) {
                }
            }
        });
        final Mailbox mailbox = mailboxFactory.createMailbox(true);
        final Actor1 actor1 = new Actor1(mailbox);
        actor1.hi1.send(boundMailbox, new ResponseProcessor<String>() {
            @Override
            public void processResponse(final String response) throws Exception {
                System.out.println(response);
                assertEquals("Hello world!", response);
            }
        });
    }
}
