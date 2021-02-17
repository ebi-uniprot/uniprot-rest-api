package org.uniprot.api.idmapping.service.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.service.IDMappingPIRService;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class CacheablePIRServiceImpl implements IDMappingPIRService {
    private static final String PIR_ID_MAPPING_URL =
            "https://idmapping.uniprot.org/cgi-bin/idmapping_http_client_async_test";
    private final RestTemplate restTemplate;

    public CacheablePIRServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Cacheable(value = "pirIDMappingCache")
    public ResponseEntity<String> doPIRRequest(IDMappingRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PIR_ID_MAPPING_URL);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(request), headers);

        return restTemplate.postForEntity(builder.toUriString(), requestBody, String.class);
    }

    private MultiValueMap<String, String> createPostBody(IDMappingRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", String.join(",", request.getIds()));
        map.add("from", request.getFrom());
        map.add("to", request.getTo());
        map.add("tax_off", request.getTaxOff());
        map.add("taxid", request.getTaxId());
        map.add("async", request.getAsync());

        return map;
    }
}
