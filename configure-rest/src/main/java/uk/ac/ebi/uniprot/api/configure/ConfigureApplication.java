package uk.ac.ebi.uniprot.api.configure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.configure","uk.ac.ebi.uniprot.rest"})
public class ConfigureApplication {
	public static void main(String[] args) {
		SpringApplication.run(ConfigureApplication.class, args);
	}
}
