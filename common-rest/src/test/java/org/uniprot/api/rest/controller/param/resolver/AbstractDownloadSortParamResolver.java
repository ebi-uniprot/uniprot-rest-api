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
            case "testDownloadWithSortByUniqueFieldJSON":
                result = getDownloadWithSortParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithSortByUniqueFieldList":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithSortByUniqueFieldTSV":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithSortByUniqueFieldOBO":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithSortByUniqueFieldXLS":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
        }
        return result;
    }

    protected DownloadParamAndResult getDownloadWithSortParamAndResult(MediaType contentType) {
        String sortOrder = "desc";
        String fieldName = getUniqueFieldName();
        return getDownloadWithSortParamAndResult(
                contentType, fieldName, sortOrder, Collections.emptyList());
    }

    protected abstract String getUniqueFieldName();

    protected abstract DownloadParamAndResult getDownloadWithSortParamAndResult(
            MediaType contentType,
            String fieldName,
            String sortOrder,
            List<String> accessionsOrder);
}
