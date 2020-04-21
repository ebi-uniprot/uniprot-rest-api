package org.uniprot.api.unisave.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.request.UniSaveRequest;
import org.uniprot.api.unisave.service.UniSaveService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@RestController
@RequestMapping("/unisave")
@Slf4j
public class UniSaveController {
    private final MessageConverterContextFactory<UniSaveEntry> converterContextFactory;
    private final UniSaveService service;

    @Autowired
    public UniSaveController(
            MessageConverterContextFactory<UniSaveEntry> converterContextFactory,
            UniSaveService service) {
        this.converterContextFactory = converterContextFactory;
        this.service = service;
    }

    @GetMapping(
            value = "/{accession}",
            produces = {APPLICATION_JSON_VALUE, FASTA_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getEntries(
            @Valid UniSaveRequest.Entries uniSaveRequest, HttpServletRequest servletRequest) {

        String acceptHeader = getHeader(servletRequest);
        setContentIfRequired(uniSaveRequest, acceptHeader);
        HttpHeaders httpHeaders =
                addDownloadHeaderIfRequired(
                        uniSaveRequest, UniProtMediaType.valueOf(acceptHeader), servletRequest);
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(acceptHeader));
        context.setEntities(service.getEntries(uniSaveRequest).stream());

        return ResponseEntity.ok().headers(httpHeaders).body(context);
    }

    @GetMapping(
            value = "/{accession}/diff",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getDiff(
            @Valid UniSaveRequest.Diff unisaveRequest, HttpServletRequest servletRequest) {
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(getHeader(servletRequest)));
        context.setEntityOnly(true);
        context.setEntities(
                Stream.of(
                        service.getDiff(
                                unisaveRequest.getAccession(),
                                unisaveRequest.getVersion1(),
                                unisaveRequest.getVersion2())));

        return ResponseEntity.ok(context);
    }

    @GetMapping(
            value = "/{accession}/status",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getStatus(
            @PathVariable String accession, HttpServletRequest servletRequest) {
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(getHeader(servletRequest)));
        context.setEntityOnly(true);
        context.setEntities(Stream.of(service.getAccessionStatus(accession)));

        return ResponseEntity.ok(context);
    }

    // responses in fasta/flatfile format will require fetching
    // content regardless of what user specifies, so set it
    private void setContentIfRequired(UniSaveRequest.Entries uniSaveRequest, String acceptHeader) {
        if (acceptHeader.equals(FASTA_MEDIA_TYPE_VALUE)
                || acceptHeader.equals(FF_MEDIA_TYPE_VALUE)) {
            uniSaveRequest.setIncludeContent(true);
        }
    }

    private String getHeader(HttpServletRequest servletRequest) {
        return servletRequest.getHeader(HttpHeaders.ACCEPT);
    }

    private HttpHeaders addDownloadHeaderIfRequired(
            UniSaveRequest.Entries request,
            MediaType contentType,
            HttpServletRequest servletRequest) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (request.isDownload()) {
            String requestQueryString = servletRequest.getQueryString();
            String queryString = requestQueryString == null ? "" : "-" + requestQueryString;
            String suffix = "." + UniProtMediaType.getFileExtension(contentType);
            httpHeaders.setContentDispositionFormData(
                    "attachment", "unisave-entries" + queryString + suffix);
            // used so that gate-way caching uses accept/accept-encoding headers as a key
            httpHeaders.add(VARY, ACCEPT);
            httpHeaders.add(VARY, ACCEPT_ENCODING);
        }
        return httpHeaders;
    }
}
