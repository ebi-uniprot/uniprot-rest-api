package uk.ac.ebi.uniprot.api.uniparc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import uk.ac.ebi.uniprot.api.common.config.CommonConfig;
import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;

/**
 * Hello world!
 *
 */

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, CommonConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.api.uniparc","uk.ac.ebi.uniprot.api.rest"})
public class UniParcRestApplication 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(UniParcRestApplication.class, args);
    }
}
