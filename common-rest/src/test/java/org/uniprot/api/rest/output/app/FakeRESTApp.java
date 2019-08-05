package org.uniprot.api.rest.output.app;

import lombok.Data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

/**
 * A fake RESTful application that returns a single configurable resource.
 *
 * Created 15/12/16
 * @author Edd
 */
@Profile("allow-origins-integration-test")
@SpringBootApplication
@Import({FakeRESTApp.FakeController.class, HttpCommonHeaderConfig.class})
public class FakeRESTApp {
    public static final String RESOURCE_1_URL = "/resource1";

    public static void main(String[] args) {
        SpringApplication.run(FakeRESTApp.class, args);
    }

    @Profile("allow-origins-integration-test")
    @RestController
    @Data
    @Import(HttpCommonHeaderConfig.class)
    static class FakeController {
        private String value;
        private final static String DEFAULT_VALUE = "value";

        FakeController() {
            this.value = DEFAULT_VALUE;
        }

        @RequestMapping(value = RESOURCE_1_URL, method = {RequestMethod.GET},
                produces = {MediaType.APPLICATION_JSON_VALUE})
        public String getResource1() {
            return "{ resource1Attribute : " + this.value+" }";
        }
    }
}