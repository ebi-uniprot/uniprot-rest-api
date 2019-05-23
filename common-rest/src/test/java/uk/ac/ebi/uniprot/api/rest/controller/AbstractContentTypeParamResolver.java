package uk.ac.ebi.uniprot.api.rest.controller;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
/**
 *
 * @author lgonzales
 */
public abstract class AbstractContentTypeParamResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        boolean result = false;
        if(parameterContext.getParameter().getType().equals(List.class) &&
                parameterContext.getParameter().getParameterizedType() != null){
            ParameterizedType paramType = (ParameterizedType) parameterContext.getParameter().getParameterizedType();
            if(paramType.getActualTypeArguments()[0].equals(ContentTypeParam.class)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        List<ContentTypeParam> result = null;
        Method method = extensionContext.getTestMethod().orElseThrow(() -> new RuntimeException("AbstractContentTypeRequestParam: Unable to find tested method"));
        switch (method.getName()) {
            case "getIdCanReturnSuccessContentType":
                result = getSuccessContentTypeRequestParamList();
                break;
            case "searchCanReturnSuccessContentType":
            case "configuredAllContentTypeForSearchSuccessEndpoint":
                result = searchSuccessContentTypeRequestParamList();
                break;
            case "getIdCanReturnBadRequestContentType":
                result = getBadRequestContentTypeRequestParamList();
                break;
            case "searchCanReturnBadRequestContentType":
            case "configuredAllContentTypeForSearchBadRequestEndpoint":
                result = searchBadRequestContentTypeRequestParamList();
                break;
            case "getIdCanReturnNotFoundRequest":
                result = getNotFoundContentTypeRequestParamList();
                break;
            case "searchCanReturnNotFoundRequest":
            case "configuredAllContentTypeForSearchNotFoundEndpoint":
                result = searchNotFoundContentTypeRequestParamList();
                break;
        }
        return result;
    }

    public abstract List<ContentTypeParam> getSuccessContentTypeRequestParamList();

    public abstract List<ContentTypeParam> getBadRequestContentTypeRequestParamList();

    public abstract List<ContentTypeParam> getNotFoundContentTypeRequestParamList();

    public abstract List<ContentTypeParam> searchSuccessContentTypeRequestParamList();

    public abstract List<ContentTypeParam> searchBadRequestContentTypeRequestParamList();

    public abstract List<ContentTypeParam> searchNotFoundContentTypeRequestParamList();
}
