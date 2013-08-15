package org.agilewiki.jactor2.util;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.ActorBase;
import org.agilewiki.jactor2.core.context.JAContext;
import org.agilewiki.jactor2.core.messaging.Request;
import org.agilewiki.jactor2.core.messaging.Transport;
import org.agilewiki.jactor2.core.processing.Mailbox;
import org.agilewiki.jactor2.core.processing.NonBlockingMailbox;

public class BoundResponseProcessorTest extends TestCase {
    public void test() throws Exception {
        final JAContext jaContext = new JAContext();
        try {
            final Driver driver = new Driver();
            driver.initialize(new NonBlockingMailbox(jaContext));
            assertEquals("Hello world!", driver.doitReq().call());
        } finally {
            jaContext.close();
        }
    }
}

class Driver extends ActorBase {
    private Request<String> doitReq;

    public Request<String> doitReq() {
        return doitReq;
    }

    @Override
    public void initialize(final Mailbox _mailbox) throws Exception {
        super.initialize(_mailbox);

        doitReq = new Request<String>(_mailbox) {
            @Override
            public void processRequest(final Transport<String> rp)
                    throws Exception {
                final BoundResponseProcessor<String> boundResponseProcessor = new BoundResponseProcessor<String>(
                        Driver.this, rp);
                final Application application = new Application(
                        boundResponseProcessor);
                application.start();
            }
        };
    }
}

class Application extends Thread {
    private final BoundResponseProcessor<String> boundResponseProcessor;

    public Application(
            final BoundResponseProcessor<String> _boundResponseProcessor) {
        boundResponseProcessor = _boundResponseProcessor;
    }

    @Override
    public void run() {
        try {
            boundResponseProcessor.processResponse("Hello world!");
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
    }
}
