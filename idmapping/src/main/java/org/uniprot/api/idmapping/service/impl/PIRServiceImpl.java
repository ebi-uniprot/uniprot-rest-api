package org.uniprot.api.idmapping.service.impl;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IDMappingPIRService;
import org.uniprot.api.idmapping.service.PIRResponseConverter;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRServiceImpl implements IDMappingPIRService {
    public static final String PIR_ID_MAPPING_URL =
            "https://idmapping.uniprot.org/cgi-bin/idmapping_http_client_async_test";
    static final HttpHeaders HTTP_HEADERS = new HttpHeaders();
    private final RestTemplate restTemplate;
    private final PIRResponseConverter pirResponseConverter;

    static {
        HTTP_HEADERS.setContentType(APPLICATION_FORM_URLENCODED);
    }

    public PIRServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.pirResponseConverter = new PIRResponseConverter();
    }

    @Override
    public IdMappingResult mapIds(IdMappingBasicRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PIR_ID_MAPPING_URL);

        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(request), HTTP_HEADERS);

        return pirResponseConverter.convertToIDMappings(
                restTemplate.postForEntity(builder.toUriString(), requestBody, String.class));
    }

    private MultiValueMap<String, String> createPostBody(IdMappingBasicRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", String.join(",", request.getIds()));
        map.add("from", request.getFrom());
        map.add("to", request.getTo());
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        return map;
    }
}
