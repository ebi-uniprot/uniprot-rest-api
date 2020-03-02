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
 * Parameter resolver for tests download n entries (a batch size) for all supported content types
 */
public abstract class AbstractDownloadSizeParamResolver extends BaseDownloadParamResolver
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
                                                "AbstractDownloadBatchParamResolver: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadLessThanDefaultBatchSizeJSON":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                MediaType.APPLICATION_JSON);
                break;
            case "testDownloadLessThanDefaultBatchSizeTSV":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeList":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeOBO":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeXLS":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE);
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
            case "testDownloadLessThanDefaultBatchSizeFF":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.FF_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeXML":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                MediaType.APPLICATION_XML);
                break;
            case "testDownloadLessThanDefaultBatchSizeFasta":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.FASTA_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeGFF":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.GFF_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeRDF":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.RDF_MEDIA_TYPE);
                break;
        }
        return result;
    }

    protected abstract DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult(
            MediaType contentType);
}
