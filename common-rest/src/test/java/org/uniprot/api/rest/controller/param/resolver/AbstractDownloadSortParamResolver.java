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
import org.uniprot.api.rest.output.UniProtMediaType;

public abstract class AbstractDownloadSortParamResolver extends BaseDownloadParamResolver
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
            case "testDownloadWithSortByAccessionJSON":
                result =
                        getDownloadWithSortParamAndResult(
                                MediaType.APPLICATION_JSON,
                                "accession",
                                "desc",
                                Collections.emptyList());
                break;
            case "testDownloadWithSortList":
                result =
                        getDownloadWithSortParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE,
                                "accession",
                                "desc",
                                Collections.emptyList());
                break;
            case "testDownloadWithSortTSV":
                result =
                        getDownloadWithSortParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE,
                                "accession",
                                "desc",
                                Collections.emptyList());
                break;
            case "testDownloadWithSortOBO":
                result =
                        getDownloadWithSortParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE,
                                "accession",
                                "desc",
                                Collections.emptyList());
                break;
            case "testDownloadWithSortXLS":
                result =
                        getDownloadWithSortParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE,
                                "accession",
                                "desc",
                                Collections.emptyList());
                break;
        }
        return result;
    }

    protected abstract DownloadParamAndResult getDownloadWithSortParamAndResult(
            MediaType contentType,
            String fieldName,
            String sortOrder,
            List<String> accessionsOrder);
}
