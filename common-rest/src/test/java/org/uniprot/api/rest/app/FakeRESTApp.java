package org.uniprot.api.rest.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

/**
 * A fake RESTful application that returns a single configurable resource.
 *
 * <p>Created 15/12/16
 *
 * @author Edd
 */
@Profile("use-fake-app")
@SpringBootApplication
@Import({FakeController.class, HttpCommonHeaderConfig.class})
@ComponentScan(basePackages = "org.uniprot.api.rest.validation.error")
public class FakeRESTApp {
    public static void main(String[] args) {
        SpringApplication.run(FakeRESTApp.class, args);
    }
}
