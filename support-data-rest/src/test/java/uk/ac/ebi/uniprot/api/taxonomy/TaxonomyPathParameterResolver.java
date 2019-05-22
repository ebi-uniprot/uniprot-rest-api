package uk.ac.ebi.uniprot.api.taxonomy;

import uk.ac.ebi.uniprot.api.rest.controller.AbstractPathParameterResolver;
import uk.ac.ebi.uniprot.api.rest.controller.PathParameter;

/**
 * @author lgonzales
 */
public class TaxonomyPathParameterResolver extends AbstractPathParameterResolver {

    public static final String TAX_ID = "9606";

    @Override
    public PathParameter getSuccessPathParameter() {
        return PathParameter.builder().pathParam(TAX_ID)
                .build();
    }

    @Override
    public PathParameter getBadRequestPathParameter() {
        return PathParameter.builder().pathParam("INVALID").build();
    }

    @Override
    public PathParameter getNotFoundPathParameter() {
        return PathParameter.builder().pathParam("1111").build();
    }

}
