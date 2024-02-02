package org.uniprot.api.rest.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadIT extends AbstractStreamControllerIT {

    @Value("${download.idFilesFolder}")
    protected String idsFolder;

    @Value("${download.resultFilesFolder}")
    protected String resultFolder;

    @Value("${async.download.queueName}")
    protected String downloadQueue;

    @Value("${async.download.retryQueueName}")
    protected String retryQueue;

    @Value(("${async.download.rejectedQueueName}"))
    protected String rejectedQueue;

    @Autowired protected AmqpAdmin amqpAdmin;

    @Container
    protected static RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
    @Container
    protected static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    protected abstract DownloadJobRepository getDownloadJobRepository();

    @DynamicPropertySource
    public static void setUpThings(DynamicPropertyRegistry propertyRegistry) {
        Startables.deepStart(rabbitMQContainer, redisContainer).join();
        assertTrue(rabbitMQContainer.isRunning());
        assertTrue(redisContainer.isRunning());
        propertyRegistry.add("spring.amqp.rabbit.port", rabbitMQContainer::getFirstMappedPort);
        propertyRegistry.add("spring.amqp.rabbit.host", rabbitMQContainer::getHost);
        System.setProperty("uniprot.redis.host", redisContainer.getHost());
        System.setProperty(
                "uniprot.redis.port", String.valueOf(redisContainer.getFirstMappedPort()));
        propertyRegistry.add("ALLOW_EMPTY_PASSWORD", () -> "yes");
    }

    protected void prepareDownloadFolders() throws IOException {
        Files.createDirectories(Path.of(this.idsFolder));
        Files.createDirectories(Path.of(this.resultFolder));
    }

    protected void uncompressFile(Path zippedFile, Path unzippedFile) throws IOException {
        InputStream fin = Files.newInputStream(zippedFile);
        BufferedInputStream in = new BufferedInputStream(fin);
        OutputStream out = Files.newOutputStream(unzippedFile);
        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
        final byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = gzIn.read(buffer))) {
            out.write(buffer, 0, n);
        }
        out.close();
        gzIn.close();
    }

    protected void cleanUpFolder(String folder) throws IOException {
        Files.list(Path.of(folder))
                .forEach(
                        path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @AfterAll
    public void cleanUpData() throws Exception {
        cleanUpFolder(this.idsFolder);
        cleanUpFolder(this.resultFolder);
        getDownloadJobRepository().deleteAll();
        this.amqpAdmin.purgeQueue(rejectedQueue, true);
        this.amqpAdmin.purgeQueue(downloadQueue, true);
        this.amqpAdmin.purgeQueue(retryQueue, true);
        rabbitMQContainer.stop();
        redisContainer.stop();
    }
}
