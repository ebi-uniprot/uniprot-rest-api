package uk.ac.ebi.uniprot.api.rest.controller.param.resolver;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;

import java.lang.reflect.Method;
/**
 *
 * @author lgonzales
 */
public abstract class AbstractSearchParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(SearchParameter.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        SearchParameter result = null;
        Method method = extensionContext.getTestMethod().orElseThrow(() -> new RuntimeException("AbstractContentTypeRequestParam: Unable to find tested method"));
        switch (method.getName()) {
            case "searchCanReturnSuccess":
                result = searchCanReturnSuccessParameter();
                break;
            case "searchCanReturnNotFound":
                result = searchCanReturnNotFoundParameter();
                break;
            case "searchAllowWildcardQueryAllDocuments":
                result = searchAllowWildcardQueryAllDocumentsParameter();
                break;
            case "searchQueryWithInvalidTypeQueryReturnBadRequest":
                result = searchQueryWithInvalidTypeQueryReturnBadRequestParameter();
                break;
            case "searchQueryWithInvalidValueQueryReturnBadRequest":
                result = searchQueryWithInvalidValueQueryReturnBadRequestParameter();
                break;
            case "searchSortWithCorrectValuesReturnSuccess":
                result = searchSortWithCorrectValuesReturnSuccessParameter();
                break;
            case "searchFieldsWithCorrectValuesReturnSuccess":
                result = searchFieldsWithCorrectValuesReturnSuccessParameter();
                break;
            case "searchFacetsWithCorrectValuesReturnSuccess":
                result = searchFacetsWithCorrectValuesReturnSuccessParameter();
                break;
        }
        return result;
    }

    protected abstract SearchParameter searchCanReturnSuccessParameter();

    protected abstract SearchParameter searchCanReturnNotFoundParameter();

    protected abstract SearchParameter searchAllowWildcardQueryAllDocumentsParameter();

    protected abstract SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter();

    protected abstract SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter();

    protected abstract SearchParameter searchSortWithCorrectValuesReturnSuccessParameter();

    protected abstract SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter();

    protected abstract SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter();

}
