package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;

public abstract class AbstractDownloadFieldsResolver extends BaseDownloadParamResolver
        implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(DownloadParamAndResult.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        DownloadParamAndResult result = null;
        Method method =
                extensionContext
                        .getTestMethod()
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AbstractDownloadParameterResolver: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadNonDefaultFieldsJSON":
                result = getDownloadNonDefaultFieldsParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadInvalidFieldsJSON":
                result = getDownloadInvalidFieldsParamAndResult(MediaType.APPLICATION_JSON);
                break;
        }
        return result;
    }

    // test for fields
    protected abstract DownloadParamAndResult getDownloadNonDefaultFieldsParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadInvalidFieldsParamAndResult(
            MediaType contentType);
}
