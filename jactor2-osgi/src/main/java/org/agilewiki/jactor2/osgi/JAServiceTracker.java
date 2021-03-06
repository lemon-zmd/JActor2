package org.agilewiki.jactor2.osgi;

import org.agilewiki.jactor2.core.blades.BladeBase;
import org.agilewiki.jactor2.core.messages.Event;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks OSGi services, reacting to OSGi's Service events, and publishes all
 * the matching services using thread-safe blade requests. The (single) listener
 * must implement ServiceChangeReceiver<SERVICE> interface.
 *
 * @param <T> The type of the service interface.
 */
public class JAServiceTracker<T> extends BladeBase implements ServiceListener,
        AutoCloseable {

    /**
     * Logger for this object.
     */
    private final Logger log = LoggerFactory.getLogger(JAServiceTracker.class);

    /**
     * The bundle context of the *processing's bundle*.
     */
    private final BundleContext bundleContext;

    /**
     * LDAP filter, used to match Service instance(s).
     */
    private final String listenerFilter;

    /**
     * The actual expected type of the service(s).
     */
    private String clazz;

    /**
     * Keeps a mapping from ServiceReferences to actual service instances.
     */
    private final HashMap<ServiceReference, T> tracked = new HashMap<ServiceReference, T>();

    /**
     * Have we received the start request?
     */
    private boolean started;

    /**
     * Have we been closed (or failed)?
     */
    private boolean closed;

    /**
     * The ServiceChangeReceiver that sent the start request.
     */
    private ServiceChangeReceiver<T> serviceChangeReceiver;

    /**
     * Creates a service tracker that matches based strictly on the service type.
     */
    public JAServiceTracker(final Reactor _reactor, final Class<?> _clazz)
            throws Exception {
        this(_reactor, Objects.requireNonNull(_clazz, "_clazz").getName());
    }

    /**
     * Creates a service tracker that matches based strictly on the service type.
     */
    public JAServiceTracker(final Reactor _reactor, final String _clazz)
            throws Exception {
        Objects.requireNonNull(_reactor, "_reactor");
        Objects.requireNonNull(_clazz, "_clazz");
        initialize(_reactor);
        // We use the bundle context of the *processing's bundle*, so that we can
        // be used in any bundle.
        bundleContext = Osgi.getBundleContext(_reactor
                .getFacility());
        // Creates a filter based on the class.
        listenerFilter = "(" + Constants.OBJECTCLASS + "=" + _clazz + ")";
        clazz = _clazz;
    }

    /**
     * Creates a service tracker that matches based an LDAP filter.
     */
    public JAServiceTracker(final Reactor _reactor, final Filter _Filter)
            throws Exception {
        Objects.requireNonNull(_reactor, "_reactor");
        Objects.requireNonNull(_Filter, "_Filter");
        initialize(_reactor);
        // We use the bundle context of the *processing's bundle*, so that we can
        // be used in any bundle.
        bundleContext = Osgi.getBundleContext(_reactor
                .getFacility());
        listenerFilter = _Filter.toString();
    }

    /**
     * Creates the start request, passing the listener as a parameter.
     */
    public void start(
            final ServiceChangeReceiver<T> _serviceChangeReceiver)
            throws Exception {
        Objects.requireNonNull(_serviceChangeReceiver, "_serviceChangeReceiver");
        new Event<JAServiceTracker<T>>() {
            @Override
            protected void processEvent(JAServiceTracker<T> _targetBlade) throws Exception {
                // We just received the start request. We can only receive one.
                if (started)
                    throw new IllegalStateException("already started");
                // Closed before even starting!
                if (closed)
                    throw new IllegalStateException("closed");
                // OK. We are started, so we can never accept this request again.
                started = true;
                // The serviceChangeReceiver, that we need to inform of changes
                serviceChangeReceiver = _serviceChangeReceiver;
                // We register ourself to get OSGi changes events.
                bundleContext.addServiceListener(JAServiceTracker.this,
                        listenerFilter);
                // We query the initial set of services matching the desired filter.
                ServiceReference[] references = null;
                if (clazz == null) {
                    // listenerFilter is NOT null, but class is
                    references = bundleContext.getServiceReferences(clazz,
                            listenerFilter);
                } else {
                    // listenerFilter is NOT null, but nor is class, and we
                    // only use of of them, so use class because it's probably
                    // faster,
                    references = bundleContext
                            .getServiceReferences(clazz, null);
                }
                int i = 0;
                // If we got any services, check them.
                if (references != null)
                    while (i < references.length) {
                        final ServiceReference ref = references[i];
                        try {
                            final T s = (T) bundleContext.getService(ref);
                            tracked.put(ref, s);
                        } catch (final Exception exception) {
                            // Service de-registered between the query and now?
                            log.error("Error retrieving service " + ref,
                                    exception);
                        }
                        i += 1;
                    }
                // Sending initial service(s) found as response.
                final Map<ServiceReference, T> m = new HashMap<ServiceReference, T>(
                        tracked);
                new ServiceChange<T>(null, m).signal(
                        serviceChangeReceiver);
            }
        }.signal(this);
    }

    /**
     * Closes the blade, and de-register itself from OSGi service tracking.
     */
    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        // De-register from OSGi service tracking events.
        bundleContext.removeServiceListener(this);
    }

    /**
     * Reacts to an OSGi service event.
     */
    @Override
    public final void serviceChanged(final ServiceEvent _event) {
        if (closed)
            // Sorry, we're closed!
            return;
        try {
            // Create service change request, to be run in our own processing,
            // because this method is not running in our blade thread.
            new Event<JAServiceTracker<T>>() {
                @Override
                protected void processEvent(JAServiceTracker<T> _targetBlade) throws Exception {
                    final int typ = _event.getType();
                    final ServiceReference ref = _event.getServiceReference();
                    switch (typ) {
                        case ServiceEvent.REGISTERED:
                            // New (?) service registration.
                            final T s = (T) bundleContext.getService(ref);
                            if (tracked.put(ref, s) != ref) {
                                // Send new service map if something changed.
                                final Map<ServiceReference, T> m = new HashMap<ServiceReference, T>(
                                        tracked);
                                new ServiceChange<T>(_event, m).signal(
                                        serviceChangeReceiver);
                            }
                            break;
                        case ServiceEvent.MODIFIED:
                            // The properties on the service reference changed. Send
                            // new service map to the listener, just in case.
                            final T sm = (T) bundleContext.getService(ref);
                            tracked.put(ref, sm);
                            final Map<ServiceReference, T> ma = new HashMap<ServiceReference, T>(
                                    tracked);
                            new ServiceChange<T>(_event, ma).signal(
                                    serviceChangeReceiver);
                            break;
                        case ServiceEvent.MODIFIED_ENDMATCH:
                        case ServiceEvent.UNREGISTERING:
                            // Service gone or no longer applies due to a change in the properties
                            //on the service reference.
                            if (tracked.remove(ref) == ref) {
                                // Send new service map if something changed.
                                final Map<ServiceReference, T> m = new HashMap<ServiceReference, T>(
                                        tracked);
                                new ServiceChange<T>(_event, m).signal(
                                        serviceChangeReceiver);
                            }
                            break;
                    }
                    // We're done processing our own request.
                }
                // doSend service change request to listener
            }.signal(this);
        } catch (final Exception exception) {
            // Most likely, a failure in signal() ...
            log.error("Unable to signal", exception);
            close(); //die a silent death without further notification.
        }
    }
}
