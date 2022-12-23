package org.uniprot.api.rest.download.repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * A fake RESTful application that returns a single configurable resource.
 *
 * <p>Created 15/12/16
 *
 * @author Edd
 */
@Profile("offline")
@SpringBootApplication
@ComponentScan(basePackages = "org.uniprot.api.rest")
public class FakeRESTApp {
    public static void main(String[] args) {
        SpringApplication.run(FakeRESTApp.class, args);
    }
}
