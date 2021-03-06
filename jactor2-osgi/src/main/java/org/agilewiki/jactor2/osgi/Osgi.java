package org.agilewiki.jactor2.osgi;

import org.agilewiki.jactor2.core.facilities.Facility;
import org.agilewiki.jactor2.core.messages.AsyncRequest;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.messages.RequestBase;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.agilewiki.jactor2.util.durable.Durables;
import org.agilewiki.jactor2.util.durable.incDes.Root;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.Version;

/**
 * A collection of static methods for integration with OSGi.
 */
final public class Osgi {

    /**
     * Returns the BundleContext saved in the bundleContext property of a Facility.
     *
     * @param _facility The processing factory.
     * @return The BundleContext.
     */
    public static BundleContext getBundleContext(final Facility _facility) {
        return (BundleContext) _facility.getProperty("bundleContext");
    }

    /**
     * Returns the version in the form major.minor.micro or major.minor.micro-qualifier.
     * This is in contrast to Version.toString, which uses a . rather than a - with a qualifier.
     *
     * @param version The version.
     * @return The formatted version.
     */
    public static String getNiceVersion(Version version) {
        int q = version.getQualifier().length();
        StringBuffer result = new StringBuffer(20 + q);
        result.append(version.getMajor());
        result.append(".");
        result.append(version.getMinor());
        result.append(".");
        result.append(version.getMicro());
        if (q > 0) {
            result.append("-");
            result.append(version.getQualifier());
        }
        return result.toString();
    }

    /**
     * Returns the OsgiFactoryLocator associated with a processing.
     *
     * @param _reactor The processing.
     * @return The OsgiFactoryLocator.
     */
    public static OsgiFactoryLocator getOsgiFactoryLocator(final Reactor _reactor) {
        return (OsgiFactoryLocator) Durables.getFactoryLocator(_reactor);
    }

    /**
     * Returns the OsgiFactoryLocator associated with a processing factory.
     *
     * @param _facility The processing factory.
     * @return The OsgiFactoryLocator.
     */
    public static OsgiFactoryLocator getOsgiFactoryLocator(final Facility _facility) {
        return (OsgiFactoryLocator) Durables.getFactoryLocator(_facility);
    }

    /**
     * Returns a filter for locating the factory locator service of another bundle.
     *
     * @param _bundleContext The current bundle context.
     * @param _bundleName    The symbolic name of the bundle of the desired factory locator service.
     * @param _niceVersion   The nice form of the version of the bundle of the desired factory locator service.
     * @return The filter.
     */
    public static Filter factoryLocatorFilter(final BundleContext _bundleContext,
                                              final String _bundleName,
                                              final String _niceVersion) throws Exception {
        return _bundleContext.createFilter("(&" +
                "(objectClass=org.agilewiki.jactor2.osgi.OsgiFactoryLocator)" +
                "(&(bundleName=" + _bundleName + ")(bundleVersion=" + _niceVersion + "))" +
                ")");
    }

    /**
     * Returns a request to create a copy of a root bound to the factory locator that can deserialize it.
     *
     * @param _root The root.
     * @return A copy of the root with the appropriate processing.
     */
    public static AsyncRequest<Root> contextCopyReq(final Root _root) throws Exception {
        return new AsyncRequest<Root>(_root.getReactor()) {
            AsyncRequest dis = this;

            @Override
            protected void processAsyncRequest() throws Exception {
                String location = _root.getBundleLocation();
                BundleContext bundleContext = getBundleContext(_root.getReactor().getFacility());
                Bundle bundle = bundleContext.installBundle(location);
                bundle.start();
                Version version = bundle.getVersion();
                LocateService<OsgiFactoryLocator> locateService = new LocateService<OsgiFactoryLocator>(
                        _root.getReactor(), OsgiFactoryLocator.class.getName());
                RequestBase.doSend(_root.getReactor(), locateService.getReq(), new AsyncResponseProcessor<OsgiFactoryLocator>() {
                    @Override
                    public void processAsyncResponse(OsgiFactoryLocator response) throws Exception {
                        Reactor newReactor = new NonBlockingReactor(response.getFacility());
                        RequestBase.doSend(_root.getReactor(), _root.copyReq(newReactor), dis);
                    }
                });
            }
        };
    }
}
