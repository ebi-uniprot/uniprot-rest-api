package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import lombok.Data;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryUtil;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.validator.impl.UniprotReturnFieldsValidator;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.validator.impl.UniprotSolrQueryFieldValidator;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/**
 * Search cursor request Entity
 *
 * Important: How to query isoforms:
 *     CANONICAL ONLY: it is the default behavior, you do not need to do anything.
 *          Implementation note: in the service layer we add a filter query(fq): is_isoform:false
 *     ALL: add request parameter includeIsoform=true
 *     ISOFORMS ONLY: Add in the request query parameter: is_isoform:true and also request parameter includeIsoform=true
 *
 * @author lgonzales
 */
@Data
public class SearchRequestDTO {
    private static final int DEFAULT_RESULTS_SIZE = 25;
    
    @NotNull(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.required}")
    @ValidSolrQuerySyntax(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = UniprotSolrQueryFieldValidator.class)
    private String query;

    @ValidReturnFields(fieldValidatorClazz = UniprotReturnFieldsValidator.class)
    private String fields;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtField.Sort.class)
    private String sort;

    private String cursor;

    @Pattern(regexp = "true|false", flags = {Pattern.Flag.CASE_INSENSITIVE}, message ="{uk.ac.ebi.uniprot.uuw.advanced.search.invalid.includeIsoform}")
    private String includeIsoform;

    @Positive(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive}")
    private Integer size = DEFAULT_RESULTS_SIZE;

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * if does not have id fields (we can not filter isoforms when querying for IDS)
     * AND
     * has includeIsoform params in the request URL
     * Then we analyze the includeIsoform request parameter.
     * IMPORTANT: Implementing this way, query search has precedence over isoform request parameter
     *
     * @return true if we need to add isoform filter query
     */
    public boolean needIsoformFilterQuery(){
        boolean result = false;
        boolean hasFieldTerms = SolrQueryUtil.hasFieldTerms(this.query,UniProtField.Search.accession.name(),
                                                                UniProtField.Search.accession_id.name(),
                                                                UniProtField.Search.mnemonic.name(),
                                                                UniProtField.Search.is_isoform.name());


        if(!hasFieldTerms && this.includeIsoform != null){
            result = !Boolean.valueOf(this.includeIsoform);
        }

        return result;
    }
}
