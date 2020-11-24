package org.uniprot.api.proteome.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.impl.JournalArticleBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.*;
import org.uniprot.core.proteome.impl.*;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lgonzales
 * @since 23/11/2020
 */
class ProteomeControllerITUtils {
    static final String UPID_PREF = "UP000005";

    @NotNull
    static ProteomeDocument getProteomeDocument(int i) {
        ProteomeEntry entry = createEntry(i);
        ProteomeDocument document = new ProteomeDocument();
        document.upid = entry.getId().getValue();
        document.organismName.add("Homo sapiens");
        document.organismName.add("human");
        document.organismTaxId = 9606;
        document.content.add(document.upid);
        document.content.addAll(document.organismName);
        document.content.add(entry.getDescription());
        document.proteomeStored = getBinary(entry);
        document.isRedundant = i % 2 != 0;
        document.isReferenceProteome = i % 2 != 0;
        document.isExcluded = i % 2 != 0;
        document.proteomeType = 1;
        document.score = 3;
        document.organismTaxon = document.organismName;
        document.taxLineageIds.add(9606);
        document.organismSort = "human";
        document.superkingdom = "eukaryota";
        document.genomeAccession.add("someAcc");
        document.genomeAssembly.add("someAcc");
        return document;
    }

    static ProteomeEntry createEntry(int i) {
        ProteomeId proteomeId = new ProteomeIdBuilder(getName(UPID_PREF, i)).build();
        String description = getName("Description", i);
        Taxonomy taxonomy =
                new TaxonomyBuilder()
                        .taxonId(9606)
                        .scientificName("Homo sapiens")
                        .mnemonic("HUMAN")
                        .build();
        LocalDate modified = LocalDate.of(2015, 11, 5);
        //	String reId = "UP000005641";
        //	ProteomeId redId = new ProteomeIdBuilder (reId).build();
        List<CrossReference<ProteomeDatabase>> xrefs = new ArrayList<>();
        CrossReference<ProteomeDatabase> xref1 =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.GENOME_ASSEMBLY)
                        .id(getName("ACA", i))
                        .build();
        xrefs.add(xref1);
        List<Component> components = new ArrayList<>();
        Component component1 =
                new ComponentBuilder()
                        .name("someName1")
                        .description("some description")
                        .proteinCount(10)
                        .build();

        Component component2 =
                new ComponentBuilder()
                        .name("someName2")
                        .description("some description 2")
                        .proteinCount(11)
                        .build();

        components.add(component1);
        components.add(component2);

        BuscoReport buscoReport =
                new BuscoReportBuilder()
                        .total(100)
                        .fragmented(110)
                        .missing(120)
                        .complete(130)
                        .completeSingle(140)
                        .completeDuplicated(150)
                        .lineageDb("lineage value")
                        .build();

        CPDReport cpdReport =
                new CPDReportBuilder()
                        .averageCdss(100)
                        .confidence(110)
                        .proteomeCount(120)
                        .status(CPDStatus.CLOSE_TO_STANDARD)
                        .stdCdss(12.3d)
                        .build();

        ProteomeCompletenessReport completenessReport =
                new ProteomeCompletenessReportBuilder()
                        .buscoReport(buscoReport)
                        .cpdReport(cpdReport)
                        .build();

        GenomeAssembly genomeAssembly =
                new GenomeAssemblyBuilder()
                        .assemblyId("assembly id")
                        .genomeAssemblyUrl("assembly url")
                        .source(GenomeAssemblySource.ENSEMBL)
                        .level(GenomeAssemblyLevel.PARTIAL)
                        .build();

        ProteomeEntryBuilder builder =
                new ProteomeEntryBuilder()
                        .proteomeId(proteomeId)
                        .description(description)
                        .taxonomy(taxonomy)
                        .modified(modified)
                        .proteomeType(ProteomeType.NORMAL)
                        .redundantTo(new ProteomeIdBuilder("UP000000001").build())
                        .strain("strain value")
                        .isolate("isolate value")
                        .citationsAdd(
                                new JournalArticleBuilder()
                                        .title("citation title")
                                        .journalName("journalName value")
                                        .build())
                        .redundantProteomesAdd(
                                new RedundantProteomeBuilder().proteomeId("UP0000000002").build())
                        .panproteome(new ProteomeIdBuilder("UP000000003").build())
                        .componentsSet(components)
                        .taxonLineagesAdd(new TaxonomyLineageBuilder().taxonId(10L).build())
                        .superkingdom(Superkingdom.EUKARYOTA)
                        .genomeAssembly(genomeAssembly)
                        .proteomeCompletenessReport(completenessReport)
                        .exclusionReasonsAdd(ExclusionReason.MIXED_CULTURE)
                        .annotationScore(15);

        return builder.build();
    }

    static String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "00" + i;
        } else if (i < 100) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    static ByteBuffer getBinary(ProteomeEntry entry) {
        try {
            return ByteBuffer.wrap(
                    ProteomeJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }
}
