package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

/**
 * Parameter resolver for tests with "query" parameter(see Child class of SearchRequest for query
 * field) for all supported content types
 */
public abstract class AbstractDownloadQueryParamResolver extends BaseDownloadParamResolver
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
            case "testDownloadByAccessionJSON":
                result = getDownloadByAccessionParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithoutQueryJSON":
                result = getDownloadWithoutQueryParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithoutQueryTSV":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithoutQueryList":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithoutQueryOBO":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithoutQueryXLS":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryJSON":
                result = getDownloadWithBadQueryParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithBadQueryList":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryTSV":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryOBO":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryXLS":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
        }
        return result;
    }

    protected abstract DownloadParamAndResult getDownloadByAccessionParamAndResult(
            MediaType contentType);

    // negative test cases
    protected abstract DownloadParamAndResult getDownloadWithoutQueryParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadWithBadQueryParamAndResult(
            MediaType contentType);
}
