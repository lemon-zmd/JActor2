package org.agilewiki.jactor2.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An activator that provides both a processing factory and a registered factory locator service.
 */
abstract public class FactoryLocatorActivator extends FacilityActivator {
    private final Logger log = LoggerFactory.getLogger(FactoryLocatorActivator.class);

    /**
     * The factory locator service.
     */
    protected OsgiFactoryLocator factoryLocator;

    @Override
    public void start(final BundleContext _bundleContext) throws Exception {
        initializeActivator(_bundleContext);
        facilityStart();
        createFactoryLocator();
        begin();
    }

    /**
     * Returns true (the default) when a config file is used to define the imports of the factory locator.
     *
     * @return True.
     */
    protected boolean configImports() {
        return true;
    }

    @Override
    protected void process() throws Exception {
        if (!configImports())
            factoryLocator.register(bundleContext);
        managedServiceRegistration();
    }

    /**
     * Creates a factory locator and adds it to the processing factory's properties.
     */
    protected void createFactoryLocator() throws Exception {
        factoryLocator = new OsgiFactoryLocator();
        factoryLocator.setFacility(getFacility());
        getFacility().putProperty("factoryLocator", factoryLocator);
    }

    @Override
    public void updated(final Dictionary<String, ?> _config) throws ConfigurationException {
        Dictionary<String, ?> oldConfig = getConfig();
        super.updated(_config);
        if (oldConfig == null && _config != null)
            configInitialized();
        else
            factoryLocator.updated(_config);

    }

    /**
     * Adds the imports to the bundle's factory locator when the first version of the
     * config file is encountered.
     *
     * @throws ConfigurationException Indicates an error with an import parameter.
     */
    protected void configInitialized() throws ConfigurationException {
        if (configImports()) {
            Dictionary<String, ?> config = getConfig();
            Enumeration<String> keys = config.keys();
            TreeMap<String, String> imports = new TreeMap<String, String>();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (!(key.startsWith("import_")))
                    continue;
                String value = (String) config.get(key);
                imports.put(key, value);
            }
            Iterator<Map.Entry<String, String>> it = imports.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String key = entry.getKey();
                String value = entry.getValue();
                int i = value.indexOf('|');
                try {
                    FactoriesImporter factoriesImporter = new FactoriesImporter(getReactor());
                    if (i > -1) {
                        String bundleName = value.substring(0, i);
                        String niceVersion = value.substring(i + 1);
                        factoriesImporter.startReq(bundleName, niceVersion).call();
                    } else {
                        factoriesImporter.startReq(value).call();
                    }
                } catch (Exception e) {
                    throw new ConfigurationException(key, "unable to process", e);
                }
            }
            factoryLocator.setEssentialService();
            factoryLocator.register(bundleContext);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        setClosing();
        getFacility().close();
    }
}
