package org.uniprot.api.support.data.disease.controller.download.IT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractDownloadControllerIT;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.disease.DiseaseSolrDocumentHelper;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

/** Class to keep things common to all disease download tests */
public class BaseDiseaseDownloadIT extends AbstractDownloadControllerIT {
    @Autowired private DiseaseRepository repository;
    public static String SEARCH_ACCESSION1 =
            "DI-" + ThreadLocalRandom.current().nextLong(10000, 50000);
    public static String SEARCH_ACCESSION2 =
            "DI-" + ThreadLocalRandom.current().nextLong(50001, 99999);
    public static List<String> SORTED_ACCESSIONS =
            new ArrayList<>(Arrays.asList(SEARCH_ACCESSION1, SEARCH_ACCESSION2));

    public static final String ACC1 = "DI-12345";
    public static final String ACC2 = "DI-11111";
    public static final String ACC3 = "DI-54321";

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.DISEASE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.disease;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getDownloadRequestPath() {
        return "/disease/download";
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        DiseaseSolrDocumentHelper.createDiseaseDocuments(this.getStoreManager(), numberOfEntries);
    }

    @Override
    protected void saveEntry(String accession, long suffix) {
        DiseaseSolrDocumentHelper.createDiseaseDocuments(this.getStoreManager(), accession, suffix);
    }

    protected static List<MediaType> getSupportedContentTypes() {
        return Arrays.asList(
                MediaType.APPLICATION_JSON,
                UniProtMediaType.TSV_MEDIA_TYPE,
                UniProtMediaType.LIST_MEDIA_TYPE,
                UniProtMediaType.XLS_MEDIA_TYPE,
                UniProtMediaType.OBO_MEDIA_TYPE);
    }
}
