package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.download.model.DownloadRequestToArrayConverter;
import org.uniprot.api.rest.download.model.HashGenerator;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 21/11/2022
 */
@RestController
public class UniProtKBDownloadController extends BasicSearchController<UniProtKBEntry> {

    private final ProducerMessageService messageService;
    private final HashGenerator<StreamRequest> hashGenerator;
    public static final String JOB_ID = "jobId";
    public static final String CONTENT_TYPE = "content-type";

    private static final String SALT_STR = "UNIPROT_DOWNLOAD_SALT"; // TODO Parametrized it

    public UniProtKBDownloadController(
            ProducerMessageService messageService,
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniProtKBEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                UNIPROTKB,
                downloadGatekeeper);
        this.messageService = messageService;
        // TODO make it a bean without new
        this.hashGenerator = new HashGenerator<>(new DownloadRequestToArrayConverter(), SALT_STR);
    }

    @GetMapping("/run") // TODO make it post to be consistent with idmapping job
    public ResponseEntity<String> submitJob(
            @ModelAttribute UniProtKBStreamRequest streamRequest, HttpServletRequest httpRequest) {
        MessageProperties messageHeader = new MessageProperties();
        String jobId = this.hashGenerator.generateHash(streamRequest);
        messageHeader.setHeader(JOB_ID, jobId);
        messageHeader.setHeader(CONTENT_TYPE, getAcceptHeader(httpRequest));
        this.messageService.sendMessage(streamRequest, messageHeader);
        return ResponseEntity.ok(jobId);
    }

    @Override
    protected String getEntityId(UniProtKBEntry entity) {
        return entity.getPrimaryAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniProtKBEntry entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
