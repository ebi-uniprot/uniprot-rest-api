package org.uniprot.api.uniparc.controller;

import java.util.List;

import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.cv.taxonomy.TaxonomicNode;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.util.TaxonomyRepoUtil;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.document.uniparc.UniParcDocumentConverter;

public class UniParcITUtils {

    private static final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();

    static UniParcDocument.UniParcDocumentBuilder getUniParcDocument(UniParcEntry entry) {
        UniParcDocumentConverter converter = new UniParcDocumentConverter();
        UniParcDocument doc = converter.convert(entry);
        UniParcDocument.UniParcDocumentBuilder builder = doc.toBuilder();
        for (UniParcCrossReference xref : entry.getUniParcCrossReferences()) {
            if (Utils.notNull(xref.getOrganism())) {
                List<TaxonomicNode> nodes =
                        TaxonomyRepoUtil.getTaxonomyLineage(
                                taxonomyRepo, (int) xref.getOrganism().getTaxonId());
                builder.organismId(nodes.get(0).id());
                builder.organismName(nodes.get(0).scientificName());
                nodes.forEach(
                        node -> {
                            builder.taxLineageId(node.id());
                            List<String> names = TaxonomyRepoUtil.extractTaxonFromNode(node);
                            names.forEach(builder::organismTaxon);
                        });
            }
        }
        return builder;
    }
}
