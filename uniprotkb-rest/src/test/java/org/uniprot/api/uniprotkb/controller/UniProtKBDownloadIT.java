package org.uniprot.api.uniprotkb.controller;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.configuration.UniprotKBConfig;
import org.uniprot.api.uniprotkb.output.MessageConverterConfig;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.ResultsConfig;
import org.uniprot.api.uniprotkb.repository.store.UniProtStoreConfig;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

import static org.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest({UniprotKBController.class})
@Import({
    DataStoreTestConfig.class,
    RepositoryConfig.class,
    UniprotFacetConfig.class,
    UniProtEntryService.class,
    UniprotQueryRepository.class,
    UniProtStoreConfig.class,
    ResultsConfig.class,
    MessageConverterConfig.class,
    UniprotKBConfig.class
})
@AutoConfigureWebClient
class UniProtKBDownloadIT {
    private static final String DOWNLOAD_RESOURCE = UNIPROTKB_RESOURCE + "/download/";
    private static final String QUERY = "query";
}
