package org.uniprot.api.idmapping.common;

import static org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.json.parser.taxonomy.TaxonomyLineageTest;
import org.uniprot.core.json.parser.uniprot.FeatureTest;
import org.uniprot.core.json.parser.uniprot.GeneLocationTest;
import org.uniprot.core.json.parser.uniprot.OrganimHostTest;
import org.uniprot.core.json.parser.uniprot.UniProtKBCrossReferenceTest;
import org.uniprot.core.json.parser.uniprot.comment.*;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.comment.Comment;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.comment.FreeTextComment;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentImpl;
import org.uniprot.core.uniprotkb.evidence.impl.EvidencedValueBuilder;
import org.uniprot.core.uniprotkb.feature.UniProtKBFeature;
import org.uniprot.core.uniprotkb.feature.UniprotKBFeatureType;
import org.uniprot.core.uniprotkb.impl.*;
import org.uniprot.core.uniprotkb.xdb.UniProtKBCrossReference;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.impl.GoEvidences;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

/**
 * @author lgonzales
 * @since 08/03/2021
 */
public class IdMappingUniProtKBITUtils {

    private IdMappingUniProtKBITUtils() {}

    public static final String UNIPROTKB_AC_ID_STR = "UniProtKB_AC-ID";
    public static final String UNIPROTKB_STR = "UniProtKB";
    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
    private static final UniProtKBEntry TEMPLATE_ISOFORM_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);

    private static final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

    public static String getUniProtKbFieldValueForValidatedField(String searchField) {
        String value = "";
        if (searchField.startsWith("ftlen_") || searchField.startsWith("xref_count_")) {
            value = "[* TO *]";
        } else {
            value =
                    switch (searchField) {
                        case "accession_id", "accession" -> "Q00011";
                        case "sec_acc" -> "B4DFC2";
                        case "mass", "length" -> "[* TO *]";
                        case "organism_id", "virus_host_id", "taxonomy_id" -> "9606";
                        case "date_modified",
                                "date_sequence_modified",
                                "date_created",
                                "lit_pubdate" -> {
                            String now = Instant.now().toString();
                            yield "[* TO " + now + "]";
                        }
                        case "proteome" -> "UP000000000";
                        case "annotation_score" -> "5";
                        case "uniref_cluster_50" -> "UniRef50_P00001";
                        case "uniref_cluster_90" -> "UniRef90_P00001";
                        case "uniref_cluster_100" -> "UniRef100_P00001";
                        case "uniparc" -> "UPI0000000001";
                        case "existence" -> "1";
                        default -> value;
                    };
        }
        return value;
    }

    public static void saveInactiveEntry(CloudSolrClient cloudSolrClient)
            throws IOException, SolrServerException {
        UniProtDocument inactiveDoc = new UniProtDocument();
        inactiveDoc.accession = "I8FBX0";
        inactiveDoc.id.add("INACTIVE_DROME");
        inactiveDoc.idInactive = "INACTIVE_DROME";
        inactiveDoc.inactiveReason = "DELETED:PROTEOME_EXCLUSION";
        inactiveDoc.deletedEntryUniParc = "UPI0001661588";
        inactiveDoc.active = false;
        cloudSolrClient.addBean(SolrCollection.uniprot.name(), inactiveDoc);
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    public static void saveEntry(
            int i, SolrClient cloudSolrClient, UniProtStoreClient<UniProtKBEntry> storeClient)
            throws IOException, SolrServerException {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("Q%05d", i);
        entryBuilder.primaryAccession(acc);
        UniProtKBEntryBuilder isoFormEntryBuilder =
                UniProtKBEntryBuilder.from(TEMPLATE_ISOFORM_ENTRY);
        if (i % 2 == 0) {
            entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            entryBuilder.uniProtId("FGFR12345_HUMAN");
            isoFormEntryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
        } else {
            entryBuilder.uniProtId(acc + "_HUMAN");
            entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            isoFormEntryBuilder.entryType(UniProtKBEntryType.TREMBL);
        }
        saveEntry(i, cloudSolrClient, storeClient, entryBuilder);

        if (i % 5 == 0) { // create isoform
            isoFormEntryBuilder.primaryAccession(acc + "-2");
            isoFormEntryBuilder.uniProtId("FGFR12345_HUMAN");
            saveEntry(i, cloudSolrClient, storeClient, isoFormEntryBuilder);
        }
    }

    public static TaxonomyDocument createTaxonomyEntry(long taxId) throws Exception {
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 1).build())
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 2).build())
                        .build();

        byte[] taxonomyObj =
                TaxonomyJsonConfig.getInstance()
                        .getFullObjectMapper()
                        .writeValueAsBytes(taxonomyEntry);

        return TaxonomyDocument.builder()
                .id(String.valueOf(taxId))
                .taxId(taxId)
                .taxonomyObj(taxonomyObj)
                .build();
    }

    private static void saveEntry(
            int i,
            SolrClient cloudSolrClient,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            UniProtKBEntryBuilder entryBuilder)
            throws IOException, SolrServerException {
        List<Comment> comments = createAllComments();
        entryBuilder.extraAttributesAdd(UNIPARC_ID_ATTRIB, "UP1234567890");
        entryBuilder.lineagesAdd(TaxonomyLineageTest.getCompleteTaxonomyLineage());
        entryBuilder.geneLocationsAdd(GeneLocationTest.getGeneLocation());
        Gene gene =
                new GeneBuilder()
                        .geneName(new GeneNameBuilder().value("gene " + i).build())
                        .orderedLocusNamesAdd(
                                new OrderedLocusNameBuilder().value("gene " + i).build())
                        .orfNamesAdd(new ORFNameBuilder().value("gene " + i).build())
                        .build();
        entryBuilder.genesAdd(gene);
        entryBuilder.organismHostsAdd(OrganimHostTest.getOrganismHost());
        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        uniProtKBEntry.getComments().addAll(comments);

        uniProtKBEntry.getUniProtKBCrossReferences().addAll(createDatabases());
        uniProtKBEntry.getFeatures().addAll(getFeatures());

        storeClient.saveEntry(uniProtKBEntry);

        UniProtDocument doc = documentConverter.convert(uniProtKBEntry);
        doc.otherOrganism = "otherValue";
        doc.modelOrganism = 9606;
        doc.organismTaxId = 9606;
        doc.taxLineageIds = List.of(9606);
        doc.organismTaxon = List.of("Human");
        doc.organismName = List.of("Human");
        doc.organismHostIds = List.of(9606);
        doc.organismHostNames = List.of("Human");
        doc.unirefCluster50 = "UniRef50_P00001";
        doc.unirefCluster90 = "UniRef90_P00001";
        doc.unirefCluster100 = "UniRef100_P00001";
        doc.uniparc = "UPI0000000001";
        doc.computationalPubmedIds.add("890123456");
        doc.fragment = true;
        doc.precursor = true;
        doc.communityPubmedIds.add("1234567");
        if (doc.accession.contains("-")) {
            doc.isIsoform = true;
        }
        doc.proteomes.add("UP000000000");
        doc.apApu.add("Search All");
        doc.commentMap.put("cc_ap_apu_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_ap_as_exp", Collections.singleton("Search All"));
        doc.apRf.add("Search All");
        doc.commentMap.put("cc_ap_rf_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_sequence_caution_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_sc_misc_exp", Collections.singleton("Search All"));
        doc.seqCautionFrameshift.add("Search All");
        doc.seqCautionErTerm.add("Search All");
        doc.seqCautionErTran.add("Search All");
        doc.seqCautionMisc.add("Search All");
        doc.rcPlasmid.add("Search All");
        doc.rcTransposon.add("Search All");
        doc.rcStrain.add("Search All");
        doc.chebi.add("Search All");
        doc.inchikey.add("Search All");
        List<String> goAssertionCodes =
                GoEvidences.INSTANCE.getEvidences().stream()
                        .filter(IdMappingUniProtKBITUtils::getManualEvidenceGroup)
                        .flatMap(IdMappingUniProtKBITUtils::getEvidenceCodes)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

        goAssertionCodes.addAll(Arrays.asList("rca", "nd", "ibd", "ikr", "ird", "unknown"));

        goAssertionCodes.forEach(
                code ->
                        doc.goWithEvidenceMaps.put(
                                "go_" + code, Collections.singleton("Search All")));
        doc.commentMap.put("cc_unknown", Collections.singleton("Search All"));
        doc.commentMap.put("cc_unknown_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_cofactor_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_interaction_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_subcellular_location_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_alternative_products_exp", Collections.singleton("Search All"));
        doc.commentMap.put("cc_webresource_exp", Collections.singleton("Search All"));
        cloudSolrClient.addBean(SolrCollection.uniprot.name(), doc);
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    private static List<Comment> createAllComments() {
        List<Comment> comments = new ArrayList<>();
        comments.add(AlternativeProductsCommentTest.getAlternativeProductsComment());
        comments.add(BPCPCommentTest.getBpcpComment());
        comments.add(CatalyticActivityCommentTest.getCatalyticActivityComment());
        comments.add(CofactorCommentTest.getCofactorComment());
        comments.add(DiseaseCommentTest.getDiseaseComment());
        comments.add(FreeTextCommentTest.getFreeTextComment());
        comments.add(FreeTextCommentTest.getFreeTextComment2());
        comments.add(InteractionCommentTest.getInteractionComment());
        comments.add(MassSpectrometryCommentTest.getMassSpectrometryComment());
        comments.add(RnaEditingCommentTest.getRnaEditingComment());
        comments.add(SequenceCautionCommentTest.getSequenceCautionComment());
        comments.add(SubcellularLocationCommentTest.getSubcellularLocationComment());
        comments.add(WebResourceCommentTest.getWebResourceComment());
        List<Comment> freeTextComments =
                Arrays.stream(CommentType.values())
                        .filter(FreeTextCommentImpl::isFreeTextCommentType)
                        .map(FreeTextCommentTest::getFreeTextComment)
                        .collect(Collectors.toList());

        FreeTextComment similarityFamily =
                new FreeTextCommentBuilder()
                        .commentType(CommentType.SIMILARITY)
                        .textsAdd(
                                new EvidencedValueBuilder()
                                        .value("Belongs to the NSMF family")
                                        .build())
                        .build();
        freeTextComments.add(similarityFamily);

        comments.addAll(freeTextComments);
        return comments;
    }

    private static boolean getManualEvidenceGroup(EvidenceGroup evidenceGroup) {
        return evidenceGroup.getGroupName().equalsIgnoreCase("Manual assertions");
    }

    private static Stream<String> getEvidenceCodes(EvidenceGroup evidenceGroup) {
        return evidenceGroup.getItems().stream().map(EvidenceItem::getCode);
    }

    private static List<UniProtKBCrossReference> createDatabases() {
        List<UniProtKBCrossReference> xrefs =
                UniProtDatabaseTypes.INSTANCE.getUniProtKBDbTypes().stream()
                        .map(UniProtDatabaseDetail::getName)
                        .map(UniProtKBCrossReferenceTest::getUniProtDBCrossReference)
                        .collect(Collectors.toList());

        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("C", "IDA"));
        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("F", "IDA"));
        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("P", "IDA"));
        return xrefs;
    }

    private static List<UniProtKBFeature> getFeatures() {
        return Arrays.stream(UniprotKBFeatureType.values())
                .map(FeatureTest::getFeature)
                .collect(Collectors.toList());
    }
}
