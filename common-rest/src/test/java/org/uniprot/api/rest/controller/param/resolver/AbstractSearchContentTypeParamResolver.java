package org.uniprot.api.rest.controller.param.resolver;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;

import java.lang.reflect.Method;

public abstract class AbstractSearchContentTypeParamResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(SearchContentTypeParam.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        SearchContentTypeParam result = null;
        Method method = extensionContext.getTestMethod().orElseThrow(() -> new RuntimeException("AbstractContentTypeRequestParam: Unable to find tested method"));
        switch (method.getName()) {
            case "searchSuccessContentTypes":
                result = searchSuccessContentTypesParam();
                break;
            case "searchBadRequestContentTypes":
                result = searchBadRequestContentTypesParam();
                break;
        }
        return result;
    }

    protected abstract SearchContentTypeParam searchSuccessContentTypesParam();

    protected abstract SearchContentTypeParam searchBadRequestContentTypesParam();

}
