package org.agilewiki.jactor2.core.reactors;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.facilities.Plant;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.messages.Blade1;
import org.agilewiki.jactor2.core.messages.RequestBase;

/**
 * Test code.
 */
public class ThreadBoundTest extends TestCase {
    ThreadBoundReactor boundReactor;
    Plant plant;

    public void testa() throws Exception {
        plant = new Plant();
        boundReactor = new ThreadBoundReactor(plant, new Runnable() {
            @Override
            public void run() {
                boundReactor.run();
                try {
                    plant.close();
                } catch (final Throwable x) {
                }
            }
        });
        final Reactor reactor = new IsolationReactor(plant);
        final Blade1 blade1 = new Blade1(reactor);
        send(blade1.hiSReq(), new AsyncResponseProcessor<String>() {
            @Override
            public void processAsyncResponse(final String response) throws Exception {
                System.out.println(response);
                assertEquals("Hello world!", response);
            }
        });
    }

    /**
     * Process the request immediately.
     *
     * @param _request        The request to be processed.
     * @param <RESPONSE_TYPE> The type of value returned.
     */
    protected <RESPONSE_TYPE> void send(final RequestBase<RESPONSE_TYPE> _request,
                                        final AsyncResponseProcessor<RESPONSE_TYPE> _responseProcessor)
            throws Exception {
        RequestBase.doSend(boundReactor, _request, _responseProcessor);
    }
}
