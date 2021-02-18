package org.uniprot.api.idmapping.service.impl;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import java.util.Objects;

import org.springframework.cache.annotation.Cacheable;
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

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class CacheablePIRServiceImpl implements IDMappingPIRService {
    public static final String PIR_ID_MAPPING_URL =
            "https://idmapping.uniprot.org/cgi-bin/idmapping_http_client_async_test";
    private final RestTemplate restTemplate;
    private final PIRResponseConverter pirResponseConverter;

    public CacheablePIRServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.pirResponseConverter = new PIRResponseConverter();
    }

    @Override
    @Cacheable(value = "pirIDMappingCache")
    public IdMappingResult doPIRRequest(IdMappingBasicRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PIR_ID_MAPPING_URL);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(request), headers);

        return pirResponseConverter.convertToIDMappings(
                restTemplate.postForEntity(builder.toUriString(), requestBody, String.class));
    }

    private MultiValueMap<String, String> createPostBody(IdMappingBasicRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        String taxOff = Objects.isNull(request.getTaxId()) ? "NO" : "YES";
        map.add("ids", String.join(",", request.getIds()));
        map.add("from", request.getFrom());
        map.add("to", request.getTo());
        map.add("tax_off", taxOff);
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        return map;
    }
}
