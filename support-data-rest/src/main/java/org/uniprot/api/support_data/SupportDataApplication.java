package org.uniprot.api.support_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.suggester.service.SuggesterServiceConfig;

@SpringBootApplication
@EnableSolrRepositories
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, SuggesterServiceConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api","org.uniprot.api.rest"})
public class SupportDataApplication {
	public static void main(String[] args) {
		SpringApplication.run(SupportDataApplication.class, args);
	}
}
