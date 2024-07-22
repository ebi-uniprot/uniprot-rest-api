package org.uniprot.api.rest.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "openapi.docs")
public class OpenAPIConfiguration {

    private String server;
}
