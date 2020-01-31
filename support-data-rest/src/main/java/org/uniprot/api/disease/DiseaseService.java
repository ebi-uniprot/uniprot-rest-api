package org.uniprot.api.disease;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.disease.DiseaseDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

@Service
@Import(DiseaseQueryBoostsConfig.class)
public class DiseaseService extends BasicSearchService<DiseaseDocument, Disease> {

    private static Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.DISEASE, "content", "accession", emptyList());

    public DiseaseService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter,
            DiseaseSolrSortClause diseaseSolrSortClause,
            QueryBoosts diseaseQueryBoosts) {

        super(
                diseaseRepository,
                toDiseaseConverter,
                diseaseSolrSortClause,
                handlerSupplier.get(),
                diseaseQueryBoosts,
                null);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.DISEASE.getField("accession").getName();
    }
}
