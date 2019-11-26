package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

public abstract class AbstractDownloadParameterResolver implements ParameterResolver {

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
            case "testDownloadAllJson":
                result = getDownloadAllParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadAllTSV":
                result = getDownloadAllParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadAllList":
                result = getDownloadAllParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadAllOBO":
                result = getDownloadAllParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadAllXLS":
                result = getDownloadAllParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSize":
                result = getDownloadLessThanDefaultBatchSizeParamAndResult();
                break;
            case "testDownloadDefaultBatchSize":
                result = getDownloadDefaultBatchSizeParamAndResult();
                break;
            case "testDownloadMoreThanBatchSize":
                result = getDownloadMoreThanBatchSizeParamAndResult();
                break;
            case "testDownloadSizeLessThanZero":
                result = getDownloadSizeLessThanZeroParamAndResult();
                break;
            case "testDownloadWithoutQuery":
                result = getDownloadWithoutQueryParamAndResult();
                break;
            case "testDownloadWithBadQuery":
                result = getDownloadWithBadQueryParamAndResult();
                break;
        }
        return result;
    }

    protected abstract DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult();

    protected abstract DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult();

    protected abstract DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult();

    protected abstract DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult();

    protected abstract DownloadParamAndResult getDownloadWithoutQueryParamAndResult();

    protected abstract DownloadParamAndResult getDownloadWithBadQueryParamAndResult();
}
