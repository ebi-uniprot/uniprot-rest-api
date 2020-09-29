package org.uniprot.api.rest.validation.config;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * In Our UniProtKB database we have some ids that has the following format
 * <prefix><colon><postfix>, for example (HGNC:3689). These Ids, are processed wrongly by solr query
 * parser, basically, it identify HGNC as a field name, causing an error in the request, because we
 * do not have a defined field named HGNC in our solr schema.
 *
 * <p>In order to sort out this problem, we created this property file with a list of trouble ids
 * that will need to be whitelisted in our field validation and handled specially in our
 * UniProtFieldQueryNodeProcessor.
 *
 * @author lgonzales
 * @since 24/09/2020
 */
@Component
@Getter
@Setter
@PropertySource({"classpath:valid-field-whitelist.properties"})
@ConfigurationProperties(prefix = "whitelist")
public class WhitelistFieldConfig {

    private Map<String, Map<String, String>> field;
}
