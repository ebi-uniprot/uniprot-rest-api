package org.uniprot.api.help.centre.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "helpcentre.contact.email")
public class ContactConfig {

    private String host;

    private String to;

    private String messageFormat;

    private Long tokenExpiresInSecs;
}
