package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;

public abstract class AbstractDownloadFieldsResolver extends BaseDownloadParamResolver
        implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        boolean paramSourceProvided =
                extensionContext
                        .getTestMethod()
                        .map(t -> t.getAnnotation(MethodSource.class) != null)
                        .orElse(false);

        return parameterContext.getParameter().getType().equals(DownloadParamAndResult.class)
                && !paramSourceProvided;
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
            case "testDownloadDefaultFieldsJSON":
                result =
                        getDownloadDefaultFieldsParamAndResult(
                                MediaType.APPLICATION_JSON, Collections.emptyList());
                break;
            case "testDownloadNonDefaultFieldsJSON":
                result =
                        getDownloadNonDefaultFieldsParamAndResult(
                                MediaType.APPLICATION_JSON,
                                Collections.emptyList(),
                                Collections.emptyList());
                break;
            case "testDownloadInvalidFieldsJSON":
                result =
                        getDownloadInvalidFieldsParamAndResult(
                                MediaType.APPLICATION_JSON, Collections.emptyList());
                break;
        }
        return result;
    }

    // test for fields
    protected abstract DownloadParamAndResult getDownloadDefaultFieldsParamAndResult(
            MediaType contentType, List<String> expectedFields);

    protected abstract DownloadParamAndResult getDownloadNonDefaultFieldsParamAndResult(
            MediaType contentType, List<String> requestedFields, List<String> expectedFields);

    protected abstract DownloadParamAndResult getDownloadInvalidFieldsParamAndResult(
            MediaType contentType, List<String> invalidRequestedFields);
}
