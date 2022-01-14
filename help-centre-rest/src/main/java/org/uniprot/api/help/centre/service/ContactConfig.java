package org.uniprot.api.help.centre.service;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "helpcentre.contact.email")
public class ContactConfig {

    private String host;

    private String port;

    private String to;

    private String messageFormat;

    private Long tokenExpireInSecs;
}
