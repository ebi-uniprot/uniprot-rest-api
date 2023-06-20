package org.uniprot.api.uniprotkb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("live")
public class UniSaveClient {

    private final RestTemplate restTemplate;

    private final String uniSaveRestEndpoint;

    UniSaveClient(@Value("${unisave.rest.endpoint}") String uniSaveRestEndpoint) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        this.restTemplate = new RestTemplate(factory);
        this.uniSaveRestEndpoint = uniSaveRestEndpoint;
    }

    public String getUniSaveHistoryVersion(String accession) {
        String url = uniSaveRestEndpoint + accession + "?format=json";
        return restTemplate.getForObject(url, String.class);
    }
}
