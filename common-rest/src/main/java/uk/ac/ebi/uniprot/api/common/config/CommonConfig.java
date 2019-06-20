package uk.ac.ebi.uniprot.api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/
@Configuration
@PropertySource( "classpath:common-message.properties")
public class CommonConfig {

}

