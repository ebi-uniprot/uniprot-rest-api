package org.uniprot.api.idmapping.service.impl;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.PIRResponseConverter;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
@Profile("live")
@Service
@Slf4j
public class PIRServiceImpl extends IdMappingPIRService {
    static final HttpHeaders HTTP_HEADERS = new HttpHeaders();

    static {
        HTTP_HEADERS.setContentType(APPLICATION_FORM_URLENCODED);
    }

    public final String pirIdMappingUrl;
    private final RestTemplate restTemplate;
    private final PIRResponseConverter pirResponseConverter;

    private Integer maxIdMappingToIdsCountEnriched;

    private Integer maxIdMappingToIdsCount;

    @Autowired
    public PIRServiceImpl(
            RestTemplate idMappingRestTemplate,
            @Value("${search.default.page.size:#{null}}") Integer defaultPageSize,
            @Value(
                            "${id.mapping.pir.url:https://idmapping.uniprot.org/cgi-bin/idmapping_http_client_async}")
                    String pirMappingUrl,
            @Value("${id.mapping.max.to.ids.count:#{null}}") Integer maxIdMappingToIdsCount,
            @Value("${id.mapping.max.to.ids.enrich.count:#{null}}")
                    Integer maxIdMappingToIdsCountEnriched) {

        super(defaultPageSize);
        this.restTemplate = idMappingRestTemplate;
        this.pirResponseConverter = new PIRResponseConverter();
        this.pirIdMappingUrl = UriComponentsBuilder.fromHttpUrl(pirMappingUrl).toUriString();
        this.maxIdMappingToIdsCountEnriched = maxIdMappingToIdsCountEnriched;
        this.maxIdMappingToIdsCount = maxIdMappingToIdsCount;
    }

    @Override
    public IdMappingResult mapIds(IdMappingJobRequest request, String jobId) {
        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(request), HTTP_HEADERS);
        log.info("Making PIR call for jobId {}", jobId);
        ResponseEntity<String> pirResponse =
                restTemplate.postForEntity(pirIdMappingUrl, requestBody, String.class);
        log.info("PIR call returned for jobId {}", jobId);
        return pirResponseConverter.convertToIDMappings(
                request,
                this.maxIdMappingToIdsCountEnriched,
                this.maxIdMappingToIdsCount,
                pirResponse);
    }

    private MultiValueMap<String, String> createPostBody(IdMappingJobRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", String.join(",", request.getIds()));

        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));

        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        return map;
    }
}
