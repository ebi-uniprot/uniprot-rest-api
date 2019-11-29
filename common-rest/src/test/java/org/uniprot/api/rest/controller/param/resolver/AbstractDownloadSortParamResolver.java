package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

public abstract class AbstractDownloadSortParamResolver extends BaseDownloadParamResolver
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
            case "testDownloadWithSortJSON":
                result = getDownloadWithSortParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithSortList":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithSortTSV":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithSortOBO":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithSortXLS":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
        }
        return result;
    }

    protected abstract DownloadParamAndResult getDownloadWithSortParamAndResult(
            MediaType contentType);
}
