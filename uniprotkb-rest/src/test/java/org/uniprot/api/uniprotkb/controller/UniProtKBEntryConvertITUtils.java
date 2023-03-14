package org.uniprot.api.uniprotkb.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.cv.taxonomy.TaxonomicNode;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.spark.indexer.uniprot.mapper.TaxonomyEntryToUniProtDocument;

import scala.Tuple2;

public class UniProtKBEntryConvertITUtils {

    static UniProtDocument aggregateTaxonomyDataToDocument(
            TaxonomyRepo taxonomyRepo, UniProtDocument document) {
        Optional<TaxonomicNode> taxNodeOpt =
                taxonomyRepo.retrieveNodeUsingTaxID(document.organismTaxId);
        if (taxNodeOpt.isPresent()) {
            TaxonomicNode node = taxNodeOpt.get();
            TaxonomyEntryToUniProtDocument taxonomyConverter = new TaxonomyEntryToUniProtDocument();
            TaxonomyEntry taxEntry =
                    new TaxonomyEntryBuilder()
                            .taxonId(node.id())
                            .scientificName(node.scientificName())
                            .synonymsAdd(node.synonymName())
                            .commonName(node.commonName())
                            .lineagesSet(getLineage(node))
                            .build();
            try {
                taxonomyConverter.call(
                        new Tuple2<>(
                                document,
                                org.apache.spark.api.java.Optional.of(List.of(taxEntry))));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Unable to aggregate taxonomy data to document", e);
            }
        }
        return document;
    }

    private static List<TaxonomyLineage> getLineage(TaxonomicNode node) {
        List<TaxonomyLineage> lineageList = new ArrayList<>();
        while (node.hasParent()) {
            node = node.parent();
            if (node.id() != 1) { // do not add root
                TaxonomyLineage item =
                        new TaxonomyLineageBuilder()
                                .taxonId(node.id())
                                .scientificName(node.scientificName())
                                .synonymsAdd(node.synonymName())
                                .commonName(node.commonName())
                                .build();
                lineageList.add(item);
            }
        }
        return lineageList;
    }
}
