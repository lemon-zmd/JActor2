package org.agilewiki.jactor2.impl;

import org.agilewiki.jactor2.api.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for mailboxes.
 */
abstract public class BaseMailbox implements JAMailbox {

    /**
     * Mailbox logger.
     */
    protected final Logger log;

    /**
     * The factory of this mailbox.
     */
    protected final JAMailboxFactory mailboxFactory;

    /**
     * The inbox, implemented as a local queue and a concurrent queue.
     */
    protected final MessageQueue inbox;

    /**
     * A reference to the thread that is executing this mailbox.
     */
    protected final AtomicReference<Thread> threadReference = new AtomicReference<Thread>();

    /**
     * The object to be run when the mailbox is emptied and before the threadReference is cleared.
     */
    private final Runnable onIdle;

    /**
     * Initial size of the outbox for each unique message destination.
     */
    private final int initialBufferSize;

    /**
     * A table of outboxes, one for each unique message destination.
     */
    protected Map<JAMailbox, ArrayDeque<Message>> sendBuffer;

    /**
     * The currently active exception handler.
     */
    private ExceptionHandler exceptionHandler;

    /**
     * The request or signal message being processed.
     */
    private Message currentMessage;

    @Override
    public boolean isIdler() {
        return onIdle != null;
    }

    @Override
    public AtomicReference<Thread> getThreadReference() {
        return threadReference;
    }

    /**
     * Create a mailbox.
     *
     * @param _onIdle            Object to be run when the inbox is emptied, or null.
     * @param _factory            The factory of this object.
     * @param _messageQueue       The inbox.
     * @param _log               The Mailbox log.
     * @param _initialBufferSize Initial size of the outbox for each unique message destination.
     */
    public BaseMailbox(final Runnable _onIdle,
                       final JAMailboxFactory _factory,
                       final MessageQueue _messageQueue,
                       final Logger _log,
                       final int _initialBufferSize) {
        onIdle = _onIdle;
        mailboxFactory = _factory;
        inbox = _messageQueue;
        log = _log;
        initialBufferSize = _initialBufferSize;
        _factory.addAutoClosable(this);
    }

    @Override
    public final boolean isEmpty() {
        return !inbox.isNonEmpty();
    }

    @Override
    public void close() throws Exception {
        if (sendBuffer == null)
            return;
        final Iterator<Entry<JAMailbox, ArrayDeque<Message>>> iter = sendBuffer
                .entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<JAMailbox, ArrayDeque<Message>> entry = iter.next();
            final JAMailbox target = entry.getKey();
            if (target.getMailboxFactory() != mailboxFactory) {
                final ArrayDeque<Message> messages = entry.getValue();
                iter.remove();
                target.addUnbufferedMessages(messages);
            } else
                iter.remove();
        }
        while (true) {
            final Message message = inbox.poll();
            if (message == null)
                return;
            if (message.isForeign() && message.isResponsePending())
                try {
                    message.close();
                } catch (final Throwable t) {
                }
        }
    }

    @Override
    public final <A extends Actor> void signal(final _Request<Void, A> _request,
                                               final A _targetActor) throws Exception {
        final Message message = inbox.createMessage(false, null, _targetActor,
                null, _request, null, EventResponseProcessor.SINGLETON);
        // No source mean never local and no buffering.
        addMessage(null, message, false);
    }

    @Override
    public final <A extends Actor> void signal(final _Request<Void, A> _request,
                                               final Mailbox _source,
                                               final A _targetActor) throws Exception {
        final MessageSource sourceMailbox = (MessageSource) _source;
        if (!sourceMailbox.isRunning())
            throw new IllegalStateException(
                    "A valid source mailbox can not be idle");
        final Message message = sourceMailbox.createMessage(false, inbox,
                _request, _targetActor, EventResponseProcessor.SINGLETON);
        boolean local = false;
        if (_source instanceof JAMailbox)
            local = this == _source ||
                    (_source != null && threadReference.get() == ((JAMailbox) _source).getThreadReference().get());
        addMessage(sourceMailbox, message, local);
    }

    @Override
    public final <E, A extends Actor> void send(final _Request<E, A> _request,
                                                final Mailbox source, final A _targetActor,
                                                final ResponseProcessor<E> _responseProcessor) throws Exception {
        final JAMailbox sourceMailbox = (JAMailbox) source;
        if (!sourceMailbox.isRunning())
            throw new IllegalStateException(
                    "A valid source mailbox can not be idle");
        final Message message = sourceMailbox.createMessage(
                this != sourceMailbox
                        && mailboxFactory != sourceMailbox.getMailboxFactory(),
                inbox, _request, _targetActor, _responseProcessor);
        addMessage(sourceMailbox, message, this == sourceMailbox ||
                (sourceMailbox != null && threadReference.get() == sourceMailbox.getThreadReference().get()));
    }

    @Override
    public boolean isRunning() {
        return threadReference.get() != null;
    }

    @Override
    public final <E, A extends Actor> Message createMessage(
            final boolean _foreign,
            final MessageQueue _inbox,
            final _Request<E, A> _request,
            final A _targetActor,
            final ResponseProcessor<E> _responseProcessor) {
        return _inbox.createMessage(_foreign, this, _targetActor, currentMessage,
                _request, exceptionHandler, _responseProcessor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <E, A extends Actor> E call(final _Request<E, A> _request,
                                             final A _targetActor) throws Exception {
        final Caller caller = new Caller();
        final Message message = inbox.createMessage(true, caller, _targetActor,
                null, _request, null,
                (ResponseProcessor<E>) DummyResponseProcessor.SINGLETON);
        addMessage(null, message, false);
        return (E) caller.call();
    }

    @Override
    public final ExceptionHandler setExceptionHandler(
            final ExceptionHandler _handler) {
        if (!isRunning())
            throw new IllegalStateException(
                    "Attempt to set an exception handler on an idle mailbox");
        final ExceptionHandler rv = this.exceptionHandler;
        this.exceptionHandler = _handler;
        return rv;
    }

    /**
     * Buffer a message to be processed later or add it to the inbox local queue for processing.
     *
     * @param _messageSource The source of the message, or null.
     * @param _message       The message to be processed or the returned results.
     * @param _local         True when the active thread controls the mailbox.
     */
    private void addMessage(final MessageSource _messageSource,
                            final Message _message,
                            final boolean _local) throws Exception {
        if ((_messageSource == null) || _local
                || !_messageSource.buffer(_message, this)) {
            addUnbufferedMessage(_message, _local);
        }
    }

    public void addUnbufferedMessage(final Message _message, final boolean _local)
            throws Exception {
        if (mailboxFactory.isClosing()) {
            if (_message.isForeign() && _message.isResponsePending())
                try {
                    _message.close();
                } catch (final Throwable t) {
                }
            return;
        }
        inbox.offer(_local, _message);
        afterAdd();
    }

    @Override
    public void addUnbufferedMessages(final Queue<Message> _messages)
            throws Exception {
        if (mailboxFactory.isClosing()) {
            final Iterator<Message> itm = _messages.iterator();
            while (itm.hasNext()) {
                final Message message = itm.next();
                if (message.isForeign() && message.isResponsePending())
                    try {
                        message.close();
                    } catch (final Throwable t) {
                    }
            }
            return;
        }
        inbox.offer(_messages);
        afterAdd();
    }

    /**
     * Called after adding some message(s) to the inbox.
     */
    abstract protected void afterAdd() throws Exception;

    @Override
    public boolean buffer(final Message _message, final JAMailbox _target) {
        if (mailboxFactory.isClosing())
            return false;
        ArrayDeque<Message> buffer;
        if (sendBuffer == null) {
            sendBuffer = new IdentityHashMap<JAMailbox, ArrayDeque<Message>>();
            buffer = null;
        } else {
            buffer = sendBuffer.get(_target);
        }
        if (buffer == null) {
            buffer = new ArrayDeque<Message>(initialBufferSize);
            sendBuffer.put(_target, buffer);
        }
        buffer.add(_message);
        return true;
    }

    @Override
    public void run() {
        while (true) {
            final Message message = inbox.poll();
            if (message == null) {
                try {
                    onIdle();
                } catch (final MigrateException me) {
                    throw me;
                } catch (Exception e) {
                    log.error("Exception thrown by onIdle", e);
                }
                if (inbox.isNonEmpty())
                    continue;
                return;
            }
            if (message.isResponsePending())
                processRequestMessage(message);
            else
                processResponseMessage(message);
        }
    }

    /**
     * Called when all pending messages have been processed.
     */
    private void onIdle() throws Exception {
        if (onIdle != null) {
            flush(true);
            onIdle.run();
        }
        flush(true);
    }

    @Override
    public final boolean flush() throws Exception {
        return flush(false);
    }

    /**
     * Flushes buffered messages, if any.
     * Returns true if there was any.
     *
     * @param _mayMigrate True when thread migration is allowed.
     * @return True when one or more buffered request/result was delivered.
     */
    abstract public boolean flush(final boolean _mayMigrate) throws Exception;

    /**
     * Process a request or signal message.
     *
     * @param _message The message to be processed.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processRequestMessage(final Message _message) {
        if (_message.isForeign())
            mailboxFactory.addAutoClosable(_message);
        beforeProcessMessage(true, _message);
        try {
            exceptionHandler = null; //NOPMD
            currentMessage = _message;
            final _Request<?, Actor> request = _message.getRequest();
            try {
                request.processRequest(_message.getTargetActor(),
                        new Transport() {
                            @Override
                            public void processResponse(final Object response)
                                    throws Exception {
                                if (_message.isForeign())
                                    mailboxFactory.removeAutoClosable(_message);
                                if (!_message.isResponsePending())
                                    return;
                                if (_message.getResponseProcessor() != EventResponseProcessor.SINGLETON) {
                                    _message.setResponse(response);
                                    _message.getMessageSource()
                                            .incomingResponse(_message,
                                                    BaseMailbox.this);
                                } else {
                                    if (response instanceof Throwable) {
                                        log.warn("Uncaught throwable",
                                                (Throwable) response);
                                    }
                                }
                            }

                            @Override
                            public MailboxFactory getMailboxFactory() {
                                MessageSource ms = _message.getMessageSource();
                                if (ms == null)
                                    return null;
                                if (!(ms instanceof Mailbox))
                                    return null;
                                return ((Mailbox) ms).getMailboxFactory();
                            }

                            @Override
                            public void processException(Exception response) throws Exception {
                                processResponse((Object) response);
                            }
                        });
            } catch (final Throwable t) {
                if (_message.isForeign())
                    mailboxFactory.removeAutoClosable(_message);
                processThrowable(t);
            }
        } finally {
            afterProcessMessage(true, _message);
        }
    }

    /**
     * Process a Throwable response.
     *
     * @param _t The Throwable response.
     */
    private void processThrowable(final Throwable _t) {
        if (!currentMessage.isResponsePending())
            return;
        final Message message = currentMessage;
        final _Request<?, Actor> req = message.getRequest();
        if (exceptionHandler != null) {
            try {
                exceptionHandler.processException(_t);
            } catch (final Throwable u) {
                log.error("Exception handler unable to process throwable "
                        + exceptionHandler.getClass().getName(), u);
                if (!(message.getResponseProcessor() instanceof EventResponseProcessor)) {
                    if (!message.isResponsePending())
                        return;
                    currentMessage.setResponse(u);
                    message.getMessageSource().incomingResponse(message,
                            BaseMailbox.this);
                } else {
                    log.error("Thrown by exception handler and uncaught "
                            + exceptionHandler.getClass().getName(), _t);
                }
            }
        } else {
            if (!message.isResponsePending())
                return;
            currentMessage.setResponse(_t);
            if (!(message.getResponseProcessor() instanceof EventResponseProcessor))
                message.getMessageSource().incomingResponse(message,
                        BaseMailbox.this);
            else {
                log.warn("Uncaught throwable", _t);
            }
        }
    }

    /**
     * Process a response message.
     *
     * @param _message A request message holding the response.
     */
    @SuppressWarnings("unchecked")
    private void processResponseMessage(final Message _message) {
        beforeProcessMessage(false, _message);
        try {
            final Object response = _message.getResponse();
            exceptionHandler = _message.getSourceExceptionHandler();
            currentMessage = _message.getOldMessage();
            if (response instanceof Throwable) {
                processThrowable((Throwable) response);
                return;
            }
            @SuppressWarnings("rawtypes")
            final ResponseProcessor responseProcessor = _message
                    .getResponseProcessor();
            try {
                responseProcessor.processResponse(response);
            } catch (final Throwable t) {
                processThrowable(t);
            }
        } finally {
            afterProcessMessage(false, _message);
        }
    }

    @Override
    public final void incomingResponse(final Message _message,
                                       final JAMailbox _responseSource) {
        try {
            addMessage(null, _message, this == _responseSource ||
                    (_responseSource != null && threadReference.get() == _responseSource.getThreadReference().get()));
        } catch (final Throwable t) {
            log.error("unable to add response message", t);
        }
    }

    @Override
    public JAMailboxFactory getMailboxFactory() {
        return mailboxFactory;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    /**
     * Called before running processXXXMessage(Message).
     *
     * @param _request True if the message does not contain a response.
     * @param _message The message about to be processed.
     */
    protected void beforeProcessMessage(final boolean _request,
                                        final Message _message) {
    }

    /**
     * Called after processing a message.
     *
     * @param _request True if the message did not previously contain a response.
     * @param _message The message that has been processed.
     */
    protected void afterProcessMessage(final boolean _request,
                                       final Message _message) {
    }
}