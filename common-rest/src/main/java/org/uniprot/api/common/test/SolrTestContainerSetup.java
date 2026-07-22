package org.uniprot.api.common.test;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.uniprot.core.util.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Follow <a
 * href="https://java.testcontainers.org/supported_docker_environment/#rancher-desktop">this
 * steps</a> if you are running with Rancher Desktop
 *
 * <pre>
 * {@code export TESTCONTAINERS_HOST_OVERRIDE=$(rdctl shell ip a show vznat | awk '/inet / {sub("/.*",""); print $2}')}
 * </pre>
 */
public class SolrTestContainerSetup implements EnvironmentPostProcessor {
    private static final Logger LOGGER = getLogger(SolrTestContainerSetup.class);

    private static volatile SolrContainer solr;
    private static final Object LOCK = new Object();

    // https://clemente-biondo.github.io/ can be used to generate hash for security.json
    private static final String SOLR_USER = "solr_admin";
    private static final String SOLR_PASS = "ajjar";

    private static final String IT_SOLR_CONTAINER_IMAGE_NAME = "it.solr-container.image-name";
    private static final String IT_SOLR_CONTAINER_ENABLED = "it.solr-container.enabled";
    private static final String IT_SOLR_CONTAINER_COMMA_SEPARATED_ZK_HOST_PROPERTIES =
            "it.solr-container.comma-separated.zk-host-properties";

    private static final String SOLR_HOST_OVERRIDE_ENV = "TESTCONTAINERS_HOST_OVERRIDE";
    private static final int FIXED_SOLR_PORT = 8983;
    private static final int FIXED_ZK_PORT = 9983;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (isContainerDisabled()) return;

        startContainerOnce();
        applySecurityJson();
        configureSystemProperties(environment, getZkHost());
    }

    private static void configureSystemProperties(
            ConfigurableEnvironment environment, String zkHost) {
        Properties properties = new Properties();
        String commaSeparatedZkHostProperties =
                System.getProperty(
                        IT_SOLR_CONTAINER_COMMA_SEPARATED_ZK_HOST_PROPERTIES,
                        "spring.data.solr.zkHost");
        Arrays.stream(commaSeparatedZkHostProperties.split(","))
                .forEach(
                        it -> {
                            properties.put(it, zkHost);
                            System.setProperty(it, zkHost);
                        });
        environment
                .getPropertySources()
                .addFirst(new PropertiesPropertySource("overrideProps", properties));
    }

    private static void startContainerOnce() {
        if (solr == null) {
            synchronized (LOCK) {
                if (solr == null) {
                    String containerImageName =
                            System.getProperty(IT_SOLR_CONTAINER_IMAGE_NAME, "solr:8.11.2");

                    String hostOverride = System.getenv(SOLR_HOST_OVERRIDE_ENV);
                    if (Utils.nullOrEmpty(hostOverride)) {
                        hostOverride = "localhost";
                    }

                    //noinspection resource
                    SolrContainer container =
                            new HostAwareSolrContainer(
                                            DockerImageName.parse(containerImageName),
                                            hostOverride,
                                            FIXED_SOLR_PORT,
                                            FIXED_ZK_PORT)
                                    .withEnv(
                                            "SOLR_AUTHENTICATION_OPTS",
                                            "-Dbasicauth=" + SOLR_USER + ":" + SOLR_PASS)
                                    .withEnv("SOLR_OPTS", "-Dsolr.data.home=/var/solr/data")
                                    .withZookeeper(true);

                    container.start();
                    solr = container;
                    LOGGER.info("Started shared Solr test container: {}", getZkHost());
                }
            }
        }
    }

    private static void applySecurityJson() {
        solr.withCopyFileToContainer(
                MountableFile.forClasspathResource("security.json"), "/tmp/security.json");
        try {
            solr.execInContainer(
                    "/opt/solr/bin/solr",
                    "zk",
                    "cp",
                    "/tmp/security.json",
                    "zk:/security.json",
                    "-z",
                    getZkHost());
        } catch (IOException e) {
            LOGGER.error("IOException while applying security.json", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException while applying security.json", e);
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isContainerDisabled() {
        String containerEnabledPropertyValue =
                System.getProperty(IT_SOLR_CONTAINER_ENABLED, String.valueOf(false));

        boolean isSolrTestContainerDisabled =
                containerEnabledPropertyValue.trim().isEmpty()
                        || !containerEnabledPropertyValue.equalsIgnoreCase("true");

        LOGGER.info(
                "Solr test container is {}. {}: {}",
                (isSolrTestContainerDisabled) ? "disabled" : "enabled",
                IT_SOLR_CONTAINER_ENABLED,
                containerEnabledPropertyValue);

        return isSolrTestContainerDisabled;
    }

    private static String getZkHost() {
        return solr.getHost() + ":" + solr.getZookeeperPort();
    }
}
