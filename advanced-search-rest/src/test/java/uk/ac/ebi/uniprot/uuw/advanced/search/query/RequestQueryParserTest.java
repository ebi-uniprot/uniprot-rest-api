package uk.ac.ebi.uniprot.uuw.advanced.search.query;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.dataservice.domain.feature.jaxb.FeatureType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RequestQueryParserTest {

    @Test
    void parseDefaultSearch() {
        String result = RequestQueryParser.parse("default search");
        assertEquals("default search",result);
    }

    @Test
    void parseOrganismRelatedIdSearch() {
        String result = RequestQueryParser.parse("organism:9606");
        assertEquals("organism_id:9606",result);
    }

    @Test
    void parseOrganismRelatedNameSearch() {
        String result = RequestQueryParser.parse("organism:\"organism value\"");
        assertEquals("organism_name:organism value",result);
    }

    @Test
    void parseOrganismRelatedNameIdSearch() {
        String result = RequestQueryParser.parse("organism:\"Homo sapiens (Human) [9606]\"");
        assertEquals("organism_id:9606",result);
    }

    @Test
    void parseRangeSearch() {
        String result = RequestQueryParser.parse("length:[1 TO 10]");
        assertEquals("length:[1 TO 10]",result);
    }

    @Test
    void parseAndSearch() {
        String result = RequestQueryParser.parse("gene:cdc7 AND taxonomy:\"Homo sapiens (Human) [9606]\"");
        assertEquals("+gene:cdc7 +taxonomy_id:9606",result);
    }

    @Test
    void parseOrSearch() {
        String result = RequestQueryParser.parse("existence:\"evidence at protein level\" OR host:\"Homo sapiens (Human) [9606]\"");
        assertEquals("existence:protein_level host_id:9606",result);
    }

    @Test
    void parseSimpleAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:biophysicochemical_properties)");
        assertEquals("cc_biophysicochemical_properties:*",result);
    }

    @Test
    void parseTermAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:biophysicochemical_properties iron)");
        assertEquals("+cc_biophysicochemical_properties:iron",result);
    }

    @Test
    void parseTermEvidenceAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:molecule_processing term evidence:ECO_0000303)");
        assertEquals("+ft_molecule_processing:term +ftev_molecule_processing:ECO_0000303",result);
    }

    @Test
    void parseTermEvidenceLengthAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:binding term length:[1 TO 10] evidence:ECO_0000250)");
        assertEquals("+ft_binding:term +ftev_binding:ECO_0000250 +ftlen_binding:[1 TO 10]",result);
    }

    @Test
    void parseAndAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:np_bind term length:[1 TO 10] evidence:ECO_0000250) " +
                "OR annotation:(type:site term length:[1 TO 10] evidence:ECO_0000250)");

        assertEquals("(+ft_np_bind:term +ftev_np_bind:ECO_0000250 +ftlen_np_bind:[1 TO 10]) " +
                "(+ft_site:term +ftev_site:ECO_0000250 +ftlen_site:[1 TO 10])",result);
    }

    @Test
    void parseIsoformAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:\"alternative splicing\" term evidence:ECO_0000250)");

        assertEquals("+cc_ap_as:term +ccev_ap_as:ECO_0000250",result);
    }

    @Test
    void parseAnnotationKineticAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:kinetic term evidence:automatic)");

        assertEquals("+cc_bpcp_kinetics:term +ccev_bpcp_kinetics:automatic",result);
    }

    @Test
    void parseNoResiduesFeatureAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:non_cons)");

        assertEquals("ft_non_cons:*",result);
    }

    @Test
    void parseFeatureSignalPeptideAnnotationSearch() {
        String result = RequestQueryParser.parse("annotation:(type:signal)");

        assertEquals("ft_signal:*",result);
    }

    @Test
    void parseSequenceCautionAnnotationSearch(){
        String result = RequestQueryParser.parse("annotation:(type:\"miscellaneous discrepancy\")");

        assertEquals("cc_sc_misc:*",result);
    }

    @Test
    void parseCitationAuthorAndJournalSearch(){
        String result = RequestQueryParser.parse("citation:(author:leo journal:leo)");

        assertEquals("lit_author:leo lit_journal:leo",result);
    }

    @Test
    void parseCitationFullSearch(){
        String result = RequestQueryParser.parse("citation:(author:Author journal:Journal published:published id:pubmed title:tittle)");

        assertEquals("lit_author:Author lit_journal:Journal lit_pubdate:published id:pubmed lit_title:tittle",result);
    }

    @Test
    void parseCofactorNoteSearch(){
        String result = RequestQueryParser.parse("cofactor:(note:term)");

        assertEquals("cc_cofactor_note:term",result);
    }

    @Test
    void parseCofactorNoteEvidenceSearch(){
        String result = RequestQueryParser.parse("cofactor:(note:\"term value\" evidence:automatic)");

        assertEquals("+cc_cofactor_note:term value +ccev_cofactor_note:automatic",result);
    }

    @Test
    void parseCofactorChebiSearch(){
        String result = RequestQueryParser.parse("cofactor:(chebi:term)");

        assertEquals("cc_cofactor_chebi:term",result);
    }

    @Test
    void parseCofactorChebiEvidenceSearch(){
        String result = RequestQueryParser.parse("cofactor:(chebi:\"phosphate [43474]\" evidence:ECO_0000269)");

        assertEquals("+cc_cofactor_chebi:phosphate [43474] +ccev_cofactor_chebi:ECO_0000269",result);
    }

    @Test
    void parseSubCelularLocationSearch(){
        String result = RequestQueryParser.parse("locations:(location:term)");

        assertEquals("cc_scl_term_location:term",result);
    }

    @Test
    void parseSubCelularLocationEvidenceSearch(){
        String result = RequestQueryParser.parse("locations:(location:term evidence:ECO_0000255)");

        assertEquals("+cc_scl_term_location:term +ccev_scl_term_location:ECO_0000255",result);
    }

    @Test
    void parseSubCelularLocationNoteSearch(){
        String result = RequestQueryParser.parse("locations:(note:term)");

        assertEquals("cc_scl_note:term",result);
    }

    @Test
    void parseSubCelularLocationNoteEvidenceSearch(){
        String result = RequestQueryParser.parse("locations:(note:term evidence:ECO_0000255)");

        assertEquals("+cc_scl_note:term +ccev_scl_note:ECO_0000255",result);
    }

    @Test
    void parseErrorWithCofactorAndLocationNoteSearch(){
        String result = RequestQueryParser.parse("cofactor:(note:cof) AND locations:(note:loc evidence:ECO_0000255)");

        assertEquals("+cc_cofactor_note:cof",result);
    }


    @Test
    void parseDatabaseSearch() {
        String result = RequestQueryParser.parse("database:(type:embl)");

        assertEquals("database:embl",result);
    }

    @Test
    void parseCrossReferenceSearch() {
        String result = RequestQueryParser.parse("database:(type:embl id:ID12345)");

        assertEquals("+xref:embl-id12345",result);
    }

}