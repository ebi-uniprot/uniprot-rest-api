package org.uniprot.api.idmapping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.model.IDMappingPair;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Created 08/02/2021
 *
 * @author Edd
 */
@Service
public class IDMappingService {
    private static final String PIR_ID_MAPPING_URL =
            "https://idmapping.uniprot.org/cgi-bin/idmapping_http_client_async_test";
    private final RestTemplate restTemplate;

    @Autowired
    public IDMappingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public QueryResult<IDMappingPair> fetchIDMappings(IDMappingRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PIR_ID_MAPPING_URL);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(request), headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(builder.toUriString(), requestBody, String.class);
        if (response.hasBody()) {
            Stream<IDMappingPair> idMappingPairStream =
                    response.getBody()
                            .lines()
                            .filter(x -> x.contains("\t"))
                            .map(row -> row.split("\t"))
                            .map(
                                    lineParts -> {
                                        IDMappingPair.IDMappingPairBuilder pairBuilder =
                                                IDMappingPair.builder().fromValue(lineParts[0]);
                                        Arrays.stream(lineParts[1].split(";"))
                                                .forEach(pairBuilder::toValue);
                                        return pairBuilder.build();
                                    });

            return QueryResult.of(idMappingPairStream, CursorPage.of(null, Integer.MAX_VALUE));
        }

        return QueryResult.of(Stream.empty(), CursorPage.of(null, 0));
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
