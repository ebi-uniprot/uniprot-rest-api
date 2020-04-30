package org.uniprot.api.rest.app;

import static org.uniprot.api.rest.app.FakeController.FAKE_RESOURCE_BASE;
import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;

import lombok.Data;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

/**
 * Created 30/04/2020
 *
 * @author Edd
 */
@Profile("use-fake-app")
@RestController
@Data
@Import(HttpCommonHeaderConfig.class)
@RequestMapping(value = FAKE_RESOURCE_BASE)
public class FakeController {
    public static final String FAKE_RESOURCE_BASE = "/fake/controller";
    public static final String FAKE_RESOURCE_1_URL = "/resource1";
    private static final String DEFAULT_VALUE = "value";
    private String value;

    FakeController() {
        this.value = DEFAULT_VALUE;
    }

    @GetMapping(
            value = FAKE_RESOURCE_1_URL,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public String getResource1() {
        return "{ resource1Attribute : " + this.value + " }";
    }

    @GetMapping(
            value = "/resource",
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE})
    public ResponseEntity<String> getResource() {
        return ResponseEntity.ok("Hello World from /resource");
    }

    @GetMapping(
            value = "/resource/{id}",
            produces = {TSV_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE})
    public ResponseEntity<String> getResourceById(@PathVariable String id) {
        return ResponseEntity.ok("Hello World from /resource/" + id);
    }
}
