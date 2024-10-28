package org.uniprot.api.rest.service.request;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "search.request.converter")
public class RequestConverterConfigProperties {

    private Integer defaultRestPageSize;

    private Integer defaultSolrPageSize;
}
