package org.uniprot.api.uniprotkb.controller.download.resolver;

import static org.uniprot.api.rest.controller.AbstractDownloadControllerIT.ENTRY_COUNT;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadAllParamResolver;

public class UniprotKBDownloadAllParamResolver extends AbstractDownloadAllParamResolver {
    @RegisterExtension
    static UniProtKBDownloadParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadParamAndResultProvider();

    @Override
    public DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType) {
        return paramAndResultProvider.getDownloadParamAndResult(contentType, ENTRY_COUNT);
    }
}
