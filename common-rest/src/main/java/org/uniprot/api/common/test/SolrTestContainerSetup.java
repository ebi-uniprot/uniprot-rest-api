package org.uniprot.api.common.test;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

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

    private SolrContainer solr;

    private static final String SOLR_USER = "solr_admin";
    private static final String SOLR_PASS = "nimda";

    private static final String IT_SOLR_CONTAINER_IMAGE_NAME = "it.solr-container.image-name";
    private static final String IT_SOLR_CONTAINER_ENABLED = "it.solr-container.enabled";
    private static final String IT_SOLR_CONTAINER_COMMA_SEPARATED_ZK_HOST_PROPERTIES =
            "it.solr-container.comma-separated.zk-host-properties";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        String containerEnabled =
                System.getProperty(IT_SOLR_CONTAINER_ENABLED, String.valueOf(false));
        if (containerEnabled.trim().isEmpty() || !containerEnabled.equalsIgnoreCase("true")) {
            LOGGER.info(
                    "Solr test container is not enabled. {}: {}",
                    IT_SOLR_CONTAINER_ENABLED,
                    containerEnabled);
            return;
        } else {
            LOGGER.info(
                    "Solr test container is enabled. {}: {}",
                    IT_SOLR_CONTAINER_ENABLED,
                    containerEnabled);
        }

        String containerImageName = System.getProperty(IT_SOLR_CONTAINER_IMAGE_NAME, "solr:9.10.1");

        solr =
                new SolrContainer(DockerImageName.parse(containerImageName))
                        .withEnv(
                                "SOLR_AUTHENTICATION_OPTS",
                                "-Dbasicauth=" + SOLR_USER + ":" + SOLR_PASS)
                        .withZookeeper(true);
        solr.start();

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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        Properties properties = new Properties();

        String commaSeparatedZkHostProperties =
                System.getProperty(
                        IT_SOLR_CONTAINER_COMMA_SEPARATED_ZK_HOST_PROPERTIES,
                        "spring.data.solr.zkHost");
        Arrays.stream(commaSeparatedZkHostProperties.split(","))
                .forEach(
                        it -> {
                            properties.put(it, getZkHost());
                            System.setProperty(it, getZkHost());
                        });
        environment
                .getPropertySources()
                .addFirst(new PropertiesPropertySource("overrideProps", properties));
    }

    private @NotNull String getZkHost() {
        return solr.getHost() + ":" + solr.getZookeeperPort();
    }

    @PreDestroy
    public void stop() {
        solr.stop();
    }
}
