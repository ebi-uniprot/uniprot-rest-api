package org.uniprot.api.idmapping.controller.utils;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.json.parser.taxonomy.TaxonomyLineageTest;
import org.uniprot.core.json.parser.uniprot.FeatureTest;
import org.uniprot.core.json.parser.uniprot.GeneLocationTest;
import org.uniprot.core.json.parser.uniprot.OrganimHostTest;
import org.uniprot.core.json.parser.uniprot.UniProtKBCrossReferenceTest;
import org.uniprot.core.json.parser.uniprot.comment.*;
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
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.impl.GoEvidences;

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
    private static final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    public static String getUniProtKbFieldValueForValidatedField(String searchField) {
        String value = "";
        if (searchField.startsWith("ftlen_") || searchField.startsWith("xref_count_")) {
            value = "[* TO *]";
        } else {
            switch (searchField) {
                case "accession_id":
                case "accession":
                    value = "Q00011";
                    break;
                case "mass":
                case "length":
                    value = "[* TO *]";
                    break;
                case "organism_id":
                case "virus_host_id":
                case "taxonomy_id":
                    value = "9606";
                    break;
                case "date_modified":
                case "date_sequence_modified":
                case "date_created":
                case "lit_pubdate":
                    String now = Instant.now().toString();
                    value = "[* TO " + now + "]";
                    break;
                case "proteome":
                    value = "UP000000000";
                    break;
                case "annotation_score":
                    value = "5";
                    break;
            }
        }
        return value;
    }

    public static void saveEntry(
            int i, SolrClient cloudSolrClient, UniProtStoreClient<UniProtKBEntry> storeClient)
            throws IOException, SolrServerException {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("Q%05d", i);
        entryBuilder.primaryAccession(acc);
        if (i % 2 == 0) {
            entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
        } else {
            entryBuilder.entryType(UniProtKBEntryType.TREMBL);
        }

        List<Comment> comments = createAllComments();
        entryBuilder.extraAttributesAdd(UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB, "UP1234567890");
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
        doc.unirefCluster50 = "UniRef50_P0001";
        doc.unirefCluster90 = "UniRef90_P0001";
        doc.unirefCluster100 = "UniRef100_P0001";
        doc.uniparc = "UPI000000000";
        doc.computationalPubmedIds.add("890123456");
        doc.communityPubmedIds.add("1234567");
        doc.isIsoform = i % 10 == 0;
        doc.proteomes.add("UP000000000");
        doc.apApu.add("Search All");
        doc.apApuEv.add("Search All");
        doc.apAsEv.add("Search All");
        doc.apRf.add("Search All");
        doc.apRfEv.add("Search All");
        doc.seqCautionFrameshift.add("Search All");
        doc.seqCautionErTerm.add("Search All");
        doc.seqCautionErTran.add("Search All");
        doc.seqCautionMisc.add("Search All");
        doc.seqCautionMiscEv.add("Search All");
        doc.rcPlasmid.add("Search All");
        doc.rcTransposon.add("Search All");
        doc.rcStrain.add("Search All");
        List<String> goAssertionCodes =
                GoEvidences.INSTANCE.getEvidences().stream()
                        .filter(IdMappingUniProtKBITUtils::getManualEvidenceGroup)
                        .flatMap(IdMappingUniProtKBITUtils::getEvidenceCodes)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

        goAssertionCodes.addAll(Arrays.asList("rca", "nd", "ibd", "ikr", "ird", "unknown", "evidence"));

        goAssertionCodes.forEach(
                code ->
                        doc.goWithEvidenceMaps.put(
                                "go_" + code, Collections.singleton("Search All")));
        Arrays.stream(CommentType.values())
                .forEach(
                        type -> {
                            String typeName = type.name().toLowerCase();
                            doc.commentEvMap.put(
                                    "ccev_" + typeName, Collections.singleton("Search All"));
                        });
        doc.commentMap.put("cc_unknown", Collections.singleton("Search All"));
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
                UniProtDatabaseTypes.INSTANCE.getAllDbTypes().stream()
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
