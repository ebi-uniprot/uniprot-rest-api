package org.uniprot.api.rest.request;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.request.FakeController.FAKE_RESOURCE;

/**
 * Created 29/04/2020
 *
 * @author Edd
 */
@RestController
@RequestMapping(value = FAKE_RESOURCE)
public class FakeController {
    static final String FAKE_RESOURCE = "/fake/controller";

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
