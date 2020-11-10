package org.uniprot.api.proteome.controller;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.core.fasta.ProteinFasta;
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.core.genecentric.Protein;
import org.uniprot.core.genecentric.impl.GeneCentricEntryBuilder;
import org.uniprot.core.genecentric.impl.ProteinBuilder;
import org.uniprot.core.json.parser.genecentric.GeneCentricJsonConfig;
import org.uniprot.core.uniprotkb.ProteinExistence;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 29/10/2020
 */
class GeneCentricControllerITUtils {

    static final String ACCESSION_PREF = "P00";
    static final String RELATED_ACCESSION_PREF_1 = "P20";
    static final String RELATED_ACCESSION_PREF_2 = "P30";
    static final String UPID = "UP000000";
    static final int TAX_ID = 9606;

    static GeneCentricDocument createDocument(String upId, int i) {
        GeneCentricEntry entry = createGeneCentricEntry(i).proteomeId(upId).build();
        return convert(entry);
    }

    static GeneCentricDocument createDocument(int i) {
        GeneCentricEntry entry = createGeneCentricEntry(i).build();
        return convert(entry);
    }

    static GeneCentricDocument convert(GeneCentricEntry entry) {
        GeneCentricDocument.GeneCentricDocumentBuilder builder = GeneCentricDocument.builder();
        List<String> accessions = new ArrayList<>();
        accessions.add(entry.getCanonicalProtein().getId());
        entry.getRelatedProteins().stream().map(ProteinFasta::getId).forEach(accessions::add);
        List<String> genes = new ArrayList<>();
        genes.add(entry.getCanonicalProtein().getGeneName());
        entry.getRelatedProteins().stream().map(Protein::getGeneName).forEach(genes::add);

        builder.accession(entry.getCanonicalProtein().getId())
                .accessions(accessions)
                .geneNames(genes)
                .reviewed(
                        entry.getCanonicalProtein().getEntryType() == UniProtKBEntryType.SWISSPROT)
                .upid(entry.getProteomeId())
                .organismTaxId(TAX_ID);
        builder.geneCentricStored(getBinary(entry));
        return builder.build();
    }

    static GeneCentricEntryBuilder createGeneCentricEntry(int i) {
        Protein protein =
                new ProteinBuilder()
                        .id(getName(ACCESSION_PREF, i))
                        .uniProtkbId("uniprotkb_id")
                        .entryType(UniProtKBEntryType.SWISSPROT)
                        .proteinName(getName("protein", i))
                        .geneName(getName("gene", i))
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("Human")
                                        .build())
                        .sequence("CCCCC")
                        .sequenceVersion(i)
                        .proteinExistence(ProteinExistence.PROTEIN_LEVEL)
                        .build();

        Protein protein2 =
                new ProteinBuilder()
                        .id(getName(RELATED_ACCESSION_PREF_1, i))
                        .uniProtkbId("uniprotkb_id")
                        .entryType(UniProtKBEntryType.SWISSPROT)
                        .proteinName(getName("aprotein", i))
                        .geneName(getName("agene", i))
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("Human")
                                        .build())
                        .sequence("BBBBB")
                        .sequenceVersion(i)
                        .proteinExistence(ProteinExistence.PROTEIN_LEVEL)
                        .build();
        Protein protein3 =
                new ProteinBuilder()
                        .id(getName(RELATED_ACCESSION_PREF_2, i))
                        .uniProtkbId("uniprotkb_id")
                        .entryType(UniProtKBEntryType.TREMBL)
                        .proteinName(getName("twoProtein", i))
                        .geneName(getName("twogene", i))
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("Human")
                                        .build())
                        .sequence("AAAAA")
                        .sequenceVersion(i)
                        .proteinExistence(ProteinExistence.PROTEIN_LEVEL)
                        .build();
        GeneCentricEntryBuilder builder = new GeneCentricEntryBuilder();

        return builder.canonicalProtein(protein)
                .proteomeId(getName(UPID, i))
                .relatedProteinsAdd(protein2)
                .relatedProteinsAdd(protein3);
    }

    private static String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "00" + i;
        } else if (i < 100) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    private static byte[] getBinary(GeneCentricEntry entry) {
        try {
            return GeneCentricJsonConfig.getInstance()
                    .getFullObjectMapper()
                    .writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }
}
