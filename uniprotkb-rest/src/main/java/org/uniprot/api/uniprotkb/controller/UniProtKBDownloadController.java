package org.uniprot.api.uniprotkb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;

/**
 * @author sahmad
 * @created 21/11/2022
 */
@RestController
public class UniProtKBDownloadController {
    private final ProducerMessageService messageService;

    public UniProtKBDownloadController(ProducerMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/run") // TODO make it post to be consistent with idmapping job
    public ResponseEntity<String> submitJob(@ModelAttribute UniProtKBStreamRequest request) {
        String jobId = this.messageService.sendMessage(request);
        return ResponseEntity.ok(jobId);
    }
}
