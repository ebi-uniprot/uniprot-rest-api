package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;

/**
 * Parameter resolver for tests download n entries (a batch size) for all supported content types
 */
public abstract class AbstractDownloadSizeParamResolver extends BaseDownloadParamResolver
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
                                                "AbstractDownloadBatchParamResolver: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadLessThanDefaultBatchSizeJSON":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                MediaType.APPLICATION_JSON);
                break;
            case "testDownloadDefaultBatchSizeJSON":
                result = getDownloadDefaultBatchSizeParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadMoreThanDefaultBatchSizeJSON":
                result = getDownloadMoreThanBatchSizeParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadSizeLessThanZeroJSON":
                result = getDownloadSizeLessThanZeroParamAndResult(MediaType.APPLICATION_JSON);
                break;
        }
        return result;
    }

    public abstract DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult(
            MediaType contentType);

    public abstract DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult(
            MediaType contentType);

    public abstract DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult(
            MediaType contentType);

    public abstract DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult(
            MediaType contentType);
}
