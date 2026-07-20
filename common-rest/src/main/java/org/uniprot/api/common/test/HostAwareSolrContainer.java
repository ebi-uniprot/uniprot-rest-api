package org.uniprot.api.common.test;

import java.util.Objects;

import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A Solr container that advertises a host reachable by the test JVM.
 *
 * <p>This works around {@link SolrContainer} configurations that advertise {@code localhost} for
 * SolrCloud/ZooKeeper. This can fail when the container runtime is remote from the host JVM, for
 * example with some Rancher Desktop configurations.
 *
 * <p>Fixed host ports are used so that the advertised Solr and ZooKeeper ports match the externally
 * reachable ports.
 */
public class HostAwareSolrContainer extends SolrContainer {

    private static final String LOCALHOST = "localhost";

    private final String advertisedHost;
    private final int fixedSolrPort;
    private final int fixedZookeeperPort;

    public HostAwareSolrContainer(
            DockerImageName dockerImageName,
            String advertisedHost,
            int fixedSolrPort,
            int fixedZookeeperPort) {

        super(dockerImageName);
        this.advertisedHost =
                Objects.requireNonNull(advertisedHost, "advertisedHost must not be null");
        if (advertisedHost.isBlank()) {
            throw new IllegalArgumentException("advertisedHost must not be blank");
        }
        validatePort(fixedSolrPort, "fixedSolrPort");
        validatePort(fixedZookeeperPort, "fixedZookeeperPort");

        this.fixedSolrPort = fixedSolrPort;
        this.fixedZookeeperPort = fixedZookeeperPort;
    }

    @Override
    protected void configure() {
        super.configure();

        patchAdvertisedHost();

        addFixedExposedPort(fixedSolrPort, SOLR_PORT);
        addFixedExposedPort(fixedZookeeperPort, ZOOKEEPER_PORT);
    }

    private void patchAdvertisedHost() {
        String[] commandParts = getCommandParts();

        if (commandParts == null) {
            return;
        }

        for (int i = 0; i < commandParts.length; i++) {
            if (LOCALHOST.equals(commandParts[i])) {
                commandParts[i] = advertisedHost;
            }
        }

        setCommandParts(commandParts);
    }

    private static void validatePort(int port, String name) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(name + " must be between 1 and 65535");
        }
    }
}
