package org.uniprot.api.idmapping.common.service.request;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.search.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtSolrSortClause;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

@Configuration
public class UniProtKBIdMappingRequestConverterConfig {

    private static final Pattern ACCESSION_REGEX_ISOFORM =
            Pattern.compile(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);

    @Bean
    public UniProtKBIdMappingRequestConverter uniProtKBIdMappingRequestConverter(
            SolrQueryConfig uniProtKBSolrQueryConf,
            UniProtSolrSortClause uniProtSolrSortClause,
            UniProtQueryProcessorConfig uniProtKBQueryProcessorConfig,
            RequestConverterConfigProperties uniProtRequestConverterConfigProperties,
            UniProtTermsConfig uniProtTermsConfig,
            FacetConfig uniProtKBFacetConfig) {
        return new UniProtKBIdMappingRequestConverter(
                uniProtKBSolrQueryConf,
                uniProtSolrSortClause,
                uniProtKBQueryProcessorConfig,
                uniProtRequestConverterConfigProperties,
                uniProtTermsConfig,
                uniProtKBFacetConfig,
                ACCESSION_REGEX_ISOFORM);
    }
}
