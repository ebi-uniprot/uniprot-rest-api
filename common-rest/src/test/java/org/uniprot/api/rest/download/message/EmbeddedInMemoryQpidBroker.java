package org.uniprot.api.rest.download.message;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.model.SystemConfig;

@Slf4j
// @TestConfiguration
public class EmbeddedInMemoryQpidBroker implements AutoCloseable {

    private static final String DEFAULT_INITIAL_CONFIGURATION_LOCATION =
            "qpid-embedded-inmemory-configuration.json";

    private boolean startupLoggedToSystemOut = true;

    private String initialConfigurationLocation = DEFAULT_INITIAL_CONFIGURATION_LOCATION;

    private URL initialConfigurationUrl;

    private SystemLauncher systemLauncher;

    public EmbeddedInMemoryQpidBroker() {
        this.systemLauncher = new SystemLauncher();
    }

    //    @PostConstruct
    public void start() throws Exception {
        this.systemLauncher.startup(createSystemConfig());
    }

    //    @PreDestroy
    public void shutdown() {
        this.systemLauncher.shutdown();
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }

    private Map<String, Object> createSystemConfig() throws IllegalConfigurationException {
        Map<String, Object> attributes = new HashMap<>();
        URL initialConfigUrl = this.initialConfigurationUrl;
        if (initialConfigUrl == null) {
            log.debug(
                    "Will attempt to load config from CLASSPATH {}",
                    this.initialConfigurationLocation);
            initialConfigUrl =
                    EmbeddedInMemoryQpidBroker.class
                            .getClassLoader()
                            .getResource(this.initialConfigurationLocation);
        }
        if (initialConfigUrl == null) {
            throw new IllegalConfigurationException(
                    "Configuration location '" + this.initialConfigurationLocation + "' not found");
        }
        attributes.put(SystemConfig.TYPE, "Memory");
        attributes.put(
                SystemConfig.INITIAL_CONFIGURATION_LOCATION, initialConfigUrl.toExternalForm());
        attributes.put(SystemConfig.STARTUP_LOGGED_TO_SYSTEM_OUT, this.startupLoggedToSystemOut);
        return attributes;
    }

    public void setInitialConfigurationLocation(String initialConfigurationLocation) {
        this.initialConfigurationLocation = initialConfigurationLocation;
    }

    public void setStartupLoggedToSystemOut(boolean startupLoggedToSystemOut) {
        this.startupLoggedToSystemOut = startupLoggedToSystemOut;
    }

    public EmbeddedInMemoryQpidBroker withInitialConfigurationLocation(
            String initialConfigurationLocation) {
        setInitialConfigurationLocation(initialConfigurationLocation);
        return this;
    }

    public EmbeddedInMemoryQpidBroker withStartupLoggedToSystemOut(boolean enabled) {
        setStartupLoggedToSystemOut(enabled);
        return this;
    }

    public void setInitialConfigurationLocation(URL initialConfigurationUrl) {
        this.initialConfigurationUrl = initialConfigurationUrl;
    }
}
