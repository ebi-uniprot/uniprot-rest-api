package uk.ac.ebi.uniprot.api.taxonomy;



import uk.ac.ebi.uniprot.api.rest.controller.AbstractQueryParameterResolver;
import uk.ac.ebi.uniprot.api.rest.controller.QueryParameter;

import java.util.Collections;

/**
 * @author lgonzales
 */
public class TaxonomyQueryParamResolver extends AbstractQueryParameterResolver {

    @Override
    public QueryParameter getFilterQueryParameter() {
        return QueryParameter.builder().build();
    }

    @Override
    public QueryParameter getInvalidFilterQueryParameter() {
        return QueryParameter.builder().build();
    }

    @Override
    public QueryParameter getSearchSuccessQueryParameter() {
        return QueryParameter.builder()
                .queryParam("query", Collections.singletonList("reference:true"))
                .build();
    }

    @Override
    public QueryParameter getSearchNotFoundQueryParameter() {
        return QueryParameter.builder()
                .queryParam("query", Collections.singletonList("tax_id:1111"))
                .build();
    }

    @Override
    public QueryParameter getSearchBadRequestQueryParameter() {
        return QueryParameter.builder()
                .queryParam("query", Collections.singletonList("tax_id:INVALID"))
                .build();
    }

}
