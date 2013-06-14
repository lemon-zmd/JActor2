package org.agilewiki.jactor3;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * A ThreadManager is used to process a collection of Runnable tasks.
 * ThreadManager is a thread pool, but with a simplified API and
 * assumes that the thread pool has a fixed number of threads.
 */
public interface ThreadManager extends Executor, AutoCloseable {
    /**
     * Create and start the concurrent.
     *
     * @param threadCount   The number of concurrent to be used.
     * @param threadFactory Used to create the concurrent.
     */
    public void start(final int threadCount, final ThreadFactory threadFactory);

    /**
     * Begin running a task.
     *
     * @param runnable The run method is to be called by another thread.
     */
    @Override
    public void execute(final Runnable runnable);

    /**
     * Stop all the threads as they complete their tasks.
     */
    @Override
    public void close();
}
