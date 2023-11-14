package org.uniprot.api.common.repository.stream.rdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RdfEntryCountProviderTest {
    private final static String UNIPROT = "uniprotkb";
    private final static String UNIPARC = "uniparc";
    private final static String UNIREF = "uniref";
    private final static String RDF = "rdf";
    private final static String TTL = "ttl";
    private final static String NT = "nt";
    private final static String UNI_PROT_RDF = "<rdf:Description rdf:about=\"P12345\">\n" +
            "dsfgfg\n" +
            "dvgbgg\n" +
            "sdww";
    private final static String UNI_PARC_RDF = "<rdf:Description rdf:about=\"UPI000012A72A\">\n" +
            "<rdf:Description rdf:about=\"UPI000012A73A\">\n" +
            "wedv\n" +
            "vbgnhh";
    private final static String UNI_REF_RDF = "<rdf:Description rdf:about=\"UniRef100_P21802\">\n" +
            "dedefg\n" +
            "<rdf:Description rdf:about=\"UniRef100_P21803\">\n" +
            "<rdf:Description rdf:about=\"UniRef100_P21804\">";
    private final static String UNI_PROT_TTL = "sdee\n" +
            "<P12345> rdf:type up:Protein ;\n" +
            "<P12346> rdf:type up:Protein ;\n" +
            "vhjmhh";
    private final static String UNI_PARC_TTL = "feef\n" +
            "<UPI000012A72A> rdf:type up:Protein ;\n" +
            "fjdiduj\n" +
            "sfe";
    private final static String UNI_REF_TTL = "<UniRef100_P21802> rdf:type up:Protein ;\n" +
            "<UniRef100_P21803> rdf:type up:Protein ;\n" +
            "<UniRef100_P21804> rdf:type up:Protein ;\n" +
            "<UniRef100_P21805> rdf:type up:Protein ;";
    private final static String UNI_PROT_NT = "kfgss\n" +
            "<http://purl.uniprot.org/uniprot/P12345> <http://purl.uniprot.org/core/reviewed>\n" +
            "<http://purl.uniprot.org/uniprot/P12346> <http://purl.uniprot.org/core/reviewed>\n" +
            "dfsdfl";
    private final static String UNI_PARC_NT = "<http://purl.uniprot.org/uniprot/UPI000012A71A> <http://purl.uniprot.org/core/reviewed>\n" +
            "dsfs\n" +
            "<http://purl.uniprot.org/uniprot/UPI000012A72A> <http://purl.uniprot.org/core/reviewed>\n" +
            "fsdfsd";
    private final static String UNI_REF_NT = "<http://purl.uniprot.org/uniprot/UniRef100_P21801> <http://purl.uniprot.org/core/reviewed>\n" +
            "scscsloi\n" +
            "<http://purl.uniprot.org/uniprot/UniRef100_P21802> <http://purl.uniprot.org/core/reviewed>\n" +
            "<http://purl.uniprot.org/uniprot/UniRef100_P21803> <http://purl.uniprot.org/core/reviewed>";
    private final RdfEntryCountProvider rdfEntryCountProvider = new RdfEntryCountProvider();

    @Test
    void getEntryCount_UniProt_RDF() {
        assertEquals(1, rdfEntryCountProvider.getEntryCount(UNI_PROT_RDF, UNIPROT, RDF));
    }

    @Test
    void getEntryCount_UniProt_TTL() {
        assertEquals(2, rdfEntryCountProvider.getEntryCount(UNI_PROT_TTL, UNIPROT, TTL));
    }

    @Test
    void getEntryCount_UniProt_NT() {
        assertEquals(2, rdfEntryCountProvider.getEntryCount(UNI_PROT_NT, UNIPROT, NT));
    }

    @Test
    void getEntryCount_UniParc_RDF() {
        assertEquals(2, rdfEntryCountProvider.getEntryCount(UNI_PARC_RDF, UNIPARC, RDF));
    }

    @Test
    void getEntryCount_UniParc_TTL() {
        assertEquals(1, rdfEntryCountProvider.getEntryCount(UNI_PARC_TTL, UNIPARC, TTL));
    }

    @Test
    void getEntryCount_UniParc_NT() {
        assertEquals(2, rdfEntryCountProvider.getEntryCount(UNI_PARC_NT, UNIPARC, NT));
    }

    @Test
    void getEntryCount_UniRef_RDF() {
        assertEquals(3, rdfEntryCountProvider.getEntryCount(UNI_REF_RDF, UNIREF, RDF));
    }

    @Test
    void getEntryCount_UniRef_TTL() {
        assertEquals(4, rdfEntryCountProvider.getEntryCount(UNI_REF_TTL, UNIREF, TTL));
    }

    @Test
    void getEntryCount_UniRef_NT() {
        assertEquals(3, rdfEntryCountProvider.getEntryCount(UNI_REF_NT, UNIREF, NT));
    }
}