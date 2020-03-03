package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

/**
 * Parameter resolver for tests download all/everything without any filter for all supported content
 * types
 */
public abstract class AbstractDownloadAllParamResolver extends BaseDownloadParamResolver
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
                                                "AbstractParamResolverForTestDownloadAll: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadAllJSON":
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
            case "testDownloadAllFF":
                result = getDownloadAllParamAndResult(UniProtMediaType.FF_MEDIA_TYPE);
                break;
            case "testDownloadAllXML":
                result = getDownloadAllParamAndResult(MediaType.APPLICATION_XML);
                break;
            case "testDownloadAllFASTA":
                result = getDownloadAllParamAndResult(UniProtMediaType.FASTA_MEDIA_TYPE);
                break;
            case "testDownloadAllGFF":
                result = getDownloadAllParamAndResult(UniProtMediaType.GFF_MEDIA_TYPE);
                break;
            case "testDownloadAllRDF":
                result = getDownloadAllParamAndResult(UniProtMediaType.RDF_MEDIA_TYPE);
                break;
        }
        return result;
    }

    public abstract DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType);
}
