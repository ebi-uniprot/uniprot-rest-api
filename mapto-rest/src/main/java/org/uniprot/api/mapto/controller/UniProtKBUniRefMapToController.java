package org.uniprot.api.mapto.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.job.JobStatusResponse;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

@RestController
@RequestMapping("/mapto/uniprotkb/uniref")
public class UniProtKBUniRefMapToController {

    public UniProtKBUniRefMapToController() {}

    @PostMapping(value = "/run")
    public ResponseEntity<JobSubmitResponse> submitMapToJob(
            @Valid @ModelAttribute UniProtKBSearchRequest request) {
        JobSubmitResponse response = null;
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/status/{jobId}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        return null;
    }

    @GetMapping(value = "/results/{jobId}")
    public ResponseEntity<MessageConverterContext<UniRefEntryLight>> getMapToEntries(
            @PathVariable String jobId, @Valid @ModelAttribute UniRefSearchRequest searchRequest) {
        return null;
    }

    @GetMapping(value = "/results/stream/{jobId}")
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRefEntryLight>>>
            streamMapToEntries(
                    @PathVariable String jobId,
                    @Valid @ModelAttribute UniRefStreamRequest streamRequest) {
        return null;
    }
}
