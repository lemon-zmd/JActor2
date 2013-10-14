import org.agilewiki.jactor2.core.blades.BladeBase;
import org.agilewiki.jactor2.core.messages.AsyncRequest;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.messages.SyncRequest;
import org.agilewiki.jactor2.core.reactors.Reactor;

public class DiningTable extends BladeBase {
    public final int seats;
    public final int meals;
    
    private int mealsEaten;
    private int[] forkUsage;
    private AsyncResponseProcessor<Boolean>[] pendingResponses;

    public DiningTable(final Reactor _reactor, final int _seats, final int _meals) 
            throws Exception {
        initialize(_reactor);
        seats = _seats;
        meals = _meals;
        forkUsage = new int[seats];
        int i = 0;
        while (i < seats) {
            forkUsage[i] = -1;
            i++;
        }
        pendingResponses = new AsyncResponseProcessor[seats];
    }
    
    private int leftFork(final int _seat) {
        return _seat;
    }
    
    private int rightFork(final int _seat) {
        return (_seat + 1) % seats;
    }
    
    private boolean isForkAvailable(final int _seat) {
        return forkUsage[_seat] == -1;
    }
    
    private boolean getForks(final int _seat) {
        int leftFork = leftFork(_seat);
        int rightFork = rightFork(_seat);
        if (isForkAvailable(leftFork) && isForkAvailable(rightFork)) {
            forkUsage[leftFork] = _seat;
            forkUsage[rightFork] = _seat;
            return true;
        }
        return false;
    }
    
    public AsyncRequest<Boolean> eatAReq(final int _seat) {
        return new AsyncBladeRequest<Boolean>() {
            final AsyncResponseProcessor<Boolean> dis = this;
            
            @Override
            protected void processAsyncRequest() throws Exception {
                if (mealsEaten == meals) {
                    dis.processAsyncResponse(false);
                    return;
                }
                
                if (getForks(_seat)) {
                    mealsEaten++;
                    dis.processAsyncResponse(true);
                    if (mealsEaten == meals) {
                        int i = 0;
                        while (i < seats) {
                            AsyncResponseProcessor<Boolean> pendingResponse = pendingResponses[i];
                            if (pendingResponse != null) {
                                pendingResponse.processAsyncResponse(false);
                            }
                            i++;
                        }
                    }
                    return;
                }
                
                pendingResponses[_seat] = dis;
            }
        };
    }
    
    private int leftSeat(final int _fork) {
        return (_fork + seats - 1) % seats;
    }
    
    private int rightSeat(final int _fork) {
        return _fork;
    }

    private void notice(final int _seat) throws Exception {
        AsyncResponseProcessor<Boolean> pendingResponse = pendingResponses[_seat];
        if (pendingResponse == null)
            return;
        if (!getForks(_seat))
            return;
        pendingResponses[_seat] = null;
        if (mealsEaten == meals)
            pendingResponse.processAsyncResponse(false);
        else {
            mealsEaten++;
            pendingResponse.processAsyncResponse(true);
        }
    }
    
    public SyncRequest<Void> ateSReq(final int _seat) {
        return new SyncBladeRequest<Void>() {
            @Override
            protected Void processSyncRequest() throws Exception {
                int leftFork = leftFork(_seat);
                int rightFork = rightFork(_seat);
                forkUsage[leftFork] = -1;
                forkUsage[rightFork] = -1;
                notice(leftSeat(leftFork));
                notice(rightSeat(rightFork));
                return null;
            }
        };
    }
}