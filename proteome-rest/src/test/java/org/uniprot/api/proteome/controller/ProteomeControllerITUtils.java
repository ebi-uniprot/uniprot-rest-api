package org.uniprot.api.proteome.controller;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.impl.JournalArticleBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.proteome.*;
import org.uniprot.core.proteome.impl.*;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.core.xml.jaxb.proteome.Proteome;
import org.uniprot.core.xml.proteome.ProteomeConverter;
import org.uniprot.store.indexer.proteome.ProteomeDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author lgonzales
 * @since 23/11/2020
 */
class ProteomeControllerITUtils {
    static final String UPID_PREF = "UP000005";

    @NotNull
    static ProteomeDocument getExcludedProteomeDocument(String upId) {
        ProteomeEntry entry =
                new ProteomeEntryBuilder()
                        .proteomeType(ProteomeType.EXCLUDED)
                        .proteomeId(upId)
                        .exclusionReasonsAdd(ExclusionReason.CONTAMINATED)
                        .build();
        ProteomeConverter converter = new ProteomeConverter();
        Proteome proteome = converter.toXml(entry);
        ProteomeDocumentConverter documentConverter =
                new ProteomeDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
        ProteomeDocument document = documentConverter.convert(proteome);
        document.proteomeStored = ByteBuffer.wrap(documentConverter.getBinaryObject(entry));
        return document;
    }

    @NotNull
    static ProteomeDocument getProteomeDocument(int i) {
        ProteomeEntry entry = createEntry(i);
        ProteomeConverter converter = new ProteomeConverter();
        Proteome proteome = converter.toXml(entry);
        ProteomeDocumentConverter documentConverter =
                new ProteomeDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
        ProteomeDocument document = documentConverter.convert(proteome);
        document.proteomeStored = ByteBuffer.wrap(documentConverter.getBinaryObject(entry));
        document.isRedundant = i % 2 != 0;
        document.isReferenceProteome = i % 2 != 0;
        document.isExcluded = false;
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
        CrossReference<ProteomeDatabase> xref =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.GENOME_ASSEMBLY)
                        .id(getName("ACA", i))
                        .build();
        xrefs.add(xref);
        xref =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.GENOME_ACCESSION)
                        .id(getName("GA", i))
                        .build();
        xrefs.add(xref);
        xref =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.BIOSAMPLE)
                        .id(getName("BS", i))
                        .build();
        xrefs.add(xref);

        GenomeAnnotation genomeAnnotation =
                new GenomeAnnotationBuilder().source("GA Value").url("GA URL value").build();
        List<Component> components = new ArrayList<>();
        Component component1 =
                new ComponentBuilder()
                        .name("someName1")
                        .description("some description")
                        .genomeAnnotation(genomeAnnotation)
                        .proteomeCrossReferencesSet(xrefs)
                        .proteinCount(10)
                        .build();

        Component component2 =
                new ComponentBuilder()
                        .name("someName2")
                        .description("some description 2")
                        .genomeAnnotation(genomeAnnotation)
                        .proteomeCrossReferencesSet(xrefs)
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
                        .genomeAnnotation(genomeAnnotation)
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
}
