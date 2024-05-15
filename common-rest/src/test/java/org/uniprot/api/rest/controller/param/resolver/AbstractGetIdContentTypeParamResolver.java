package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;

/**
 * @author lgonzales
 */
public abstract class AbstractGetIdContentTypeParamResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(GetIdContentTypeParam.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        GetIdContentTypeParam result = null;
        Method method =
                extensionContext
                        .getTestMethod()
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AbstractContentTypeRequestParam: Unable to find tested method"));
        switch (method.getName()) {
            case "idSuccessContentTypes":
                result = idSuccessContentTypesParam();
                break;
            case "idBadRequestContentTypes":
                result = idBadRequestContentTypesParam();
                break;
        }
        return result;
    }

    protected abstract GetIdContentTypeParam idSuccessContentTypesParam();

    protected abstract GetIdContentTypeParam idBadRequestContentTypesParam();
}
