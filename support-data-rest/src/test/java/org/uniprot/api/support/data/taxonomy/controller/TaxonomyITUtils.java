package org.uniprot.api.support.data.taxonomy.controller;

import java.util.Collections;
import java.util.List;

import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.*;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author sahmad
 * @created 23/01/2021
 */
public class TaxonomyITUtils {
    public static TaxonomyDocument createSolrDoc(long taxId, boolean facet) {
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .scientificName("scientific" + taxId)
                        .mnemonic("mnemonic" + taxId)
                        .commonName("common" + taxId)
                        .synonymsAdd("synonym" + taxId)
                        .otherNamesAdd("other names" + taxId)
                        .rank(TaxonomyRank.FAMILY)
                        .parent(
                                new TaxonomyBuilder()
                                        .taxonId(taxId - 1)
                                        .scientificName("name" + (taxId - 1))
                                        .commonName("commonname" + (taxId - 1))
                                        .build())
                        .statistics(new TaxonomyStatisticsBuilder().build())
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 1).build())
                        .strainsAdd(new TaxonomyStrainBuilder().name("str name").build())
                        .hostsAdd(new TaxonomyBuilder().taxonId(taxId + 2).build())
                        .linksAdd("link value")
                        .active(true)
                        .inactiveReason(new TaxonomyInactiveReasonBuilder().build())
                        .build();

        TaxonomyDocument.TaxonomyDocumentBuilder docBuilder =
                TaxonomyDocument.builder()
                        .id(String.valueOf(taxId))
                        .taxId(taxId)
                        .parent(taxId - 1)
                        .synonym("synonym" + taxId)
                        .scientific("scientific" + taxId)
                        .common("common " + taxId)
                        .mnemonic("mnemonic" + taxId)
                        .rank("rank")
                        .otherNames(Collections.singletonList("otherName"))
                        .strain(Collections.singletonList("strain"))
                        .host(Collections.singletonList(10L))
                        .ancestor(Collections.singletonList(10L + 1L))
                        .linked(true)
                        .active(true)
                        .taxonomyObj(getTaxonomyBinary(taxonomyEntry));
        if (facet) {
            docBuilder.taxonomiesWith(
                    List.of(
                            "5_proteome",
                            "4_reference",
                            "3_unreviewed",
                            "2_reviewed",
                            "1_uniprotkb"));
            docBuilder.superkingdom("Bacteria");
        }
        return docBuilder.build();
    }

    public static byte[] getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    public static TaxonomyDocument createInactiveTaxonomySolrDoc(long taxId) {
        TaxonomyEntry inactiveEntry =
                new TaxonomyEntryBuilder().taxonId(taxId).active(false).build();
        TaxonomyDocument inactiveDoc =
                TaxonomyDocument.builder()
                        .id(String.valueOf(taxId))
                        .taxId(taxId)
                        .active(false)
                        .taxonomyObj(getTaxonomyBinary(inactiveEntry))
                        .build();
        return inactiveDoc;
    }
}
