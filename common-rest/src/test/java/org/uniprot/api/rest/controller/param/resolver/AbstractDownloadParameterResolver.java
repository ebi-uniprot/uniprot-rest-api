package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.uniprot.api.rest.controller.param.SearchParameter;

public abstract class AbstractDownloadParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(SearchParameter.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        SearchParameter result = null;
        Method method =
                extensionContext
                        .getTestMethod()
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AbstractDownloadParameterResolver: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadAll":
                result = downloadAllParameter();
                break;
            case "testDownloadLessThanDefaultBatchSize":
                result = downloadLessThanDefaultBatchSizeParameter();
                break;
            case "testDownloadDefaultBatchSize":
                result = downloadDefaultBatchSizeParameter();
                break;
            case "testDownloadMoreThanBatchSize":
                result = downloadMoreThanBatchSizeParameter();
                break;
            case "testDownloadSizeLessThanZero":
                result = downloadSizeLessThanZeroParameter();
                break;
            case "testDownloadWithoutQuery":
                result = downloadWithoutQueryParameter();
                break;
            case "testDownloadWithBadQuery":
                result = downloadWithBadQueryParameter();
                break;
        }
        return result;
    }

    protected abstract SearchParameter downloadAllParameter();

    protected abstract SearchParameter downloadLessThanDefaultBatchSizeParameter();

    protected abstract SearchParameter downloadDefaultBatchSizeParameter();

    protected abstract SearchParameter downloadMoreThanBatchSizeParameter();

    protected abstract SearchParameter downloadSizeLessThanZeroParameter();

    protected abstract SearchParameter downloadWithoutQueryParameter();

    protected abstract SearchParameter downloadWithBadQueryParameter();
}
