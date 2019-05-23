package uk.ac.ebi.uniprot.api.rest.controller;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
/**
 *
 * @author lgonzales
 */
public abstract class AbstractQueryParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(QueryParameter.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        QueryParameter result = null;
        Method method = extensionContext.getTestMethod().orElseThrow(() -> new RuntimeException("AbstractContentTypeRequestParam: Unable to find tested method"));
        switch (method.getName()) {
            case "getIdCanFilterFieldsEntryReturnSuccess":
                result = getFilterQueryParameter();
                break;
            case "getIdInvalidFilterFieldsEntryReturnBadRequest":
                result = getInvalidFilterQueryParameter();
                break;
            case "searchCanReturnSuccessContentType":
                result = getSearchSuccessQueryParameter();
                break;
            case "searchCanReturnNotFoundRequest":
                result = getSearchNotFoundQueryParameter();
                break;
            case "searchCanReturnBadRequestContentType":
                result = getSearchBadRequestQueryParameter();
                break;
            case "searchFacetsForXMLFormatReturnBadRequest":
                break;
            case "searchSortWithCorrectValuesReturnSuccess":
                break;
            case "searchQueryWithInvalidValueQueryReturnBadRequest":
                break;
            case "searchQueryWithInvalidTypeQueryReturnBadRequest":
                break;
            case "searchFieldsWithCorrectValuesReturnSuccess":
                break;
            case "searchAllowWildcardQueryAllDocuments":
                break;
            case "searchFacetsWithCorrectValuesReturnSuccess":
                break;
        }
        return result;
    }

    public abstract QueryParameter getFilterQueryParameter();

    public abstract QueryParameter getInvalidFilterQueryParameter();

    public abstract QueryParameter getSearchSuccessQueryParameter();

    public abstract QueryParameter getSearchNotFoundQueryParameter();

    public abstract QueryParameter getSearchBadRequestQueryParameter();

}
