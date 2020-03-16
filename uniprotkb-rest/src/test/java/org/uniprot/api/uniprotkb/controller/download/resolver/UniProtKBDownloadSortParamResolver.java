package org.uniprot.api.uniprotkb.controller.download.resolver;

import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadSortParamResolver;

public class UniProtKBDownloadSortParamResolver extends AbstractDownloadSortParamResolver {

    @RegisterExtension
    static UniProtKBDownloadSortParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadSortParamAndResultProvider();

    @Override
    public DownloadParamAndResult getDownloadWithSortParamAndResult(
            MediaType contentType,
            String fieldName,
            String sortOrder,
            List<String> expectedAccessions) {
        DownloadParamAndResult paramAndResult =
                paramAndResultProvider.getDownloadParamAndResultForSort(
                        contentType, fieldName, sortOrder, 3, expectedAccessions);
        return paramAndResult;
    }

    @Override
    protected String getUniqueFieldName() {
        return "accession";
    }
}
