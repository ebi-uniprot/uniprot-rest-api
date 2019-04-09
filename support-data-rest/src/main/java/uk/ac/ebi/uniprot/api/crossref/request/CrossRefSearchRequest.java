package uk.ac.ebi.uniprot.api.crossref.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.crossref.config.CrossRefFacetConfig;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRefValidSortFields;
import uk.ac.ebi.uniprot.api.crossref.validator.CrossRefSolrQueryFieldValidator;
import uk.ac.ebi.uniprot.api.rest.validation.*;
import uk.ac.ebi.uniprot.common.Utils;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
public class CrossRefSearchRequest {
    private static final int DEFAULT_RESULTS_SIZE = 25;

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = CrossRefSolrQueryFieldValidator.class)
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = CrossRefValidSortFields.class)
    private String sort;

    private String cursor;

    @ValidFacets(facetConfig = CrossRefFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size = DEFAULT_RESULTS_SIZE;

    public List<String> getFacetList(){
        if(hasFacets()) {
            return Arrays.asList(facets.split(("\\s*,\\s*")));
        } else {
            return Collections.emptyList();
        }
    }

    public boolean hasFacets() {
        return Utils.notEmpty(facets);
    }

}
