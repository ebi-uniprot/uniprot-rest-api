package uk.ac.ebi.uniprot.api.uniparc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;
import uk.ac.ebi.uniprot.api.rest.validation.error.ErrorHandlerConfig;

/**
 * Hello world!
 *
 */

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.api.uniparc","uk.ac.ebi.uniprot.api.rest"})
public class UniParcRestApplication 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(UniParcRestApplication.class, args);
    }
}
