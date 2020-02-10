package org.uniprot.api.disease;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.uniprot.api.disease.response.converter.DiseaseOBOMessageConverter;
import org.uniprot.core.cv.disease.CrossReference;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.core.cv.disease.DiseaseBuilder;

class DiseaseOBOConverterTest {
    private static final String NOW = OBOFormatConstants.headerDateFormat().format(new Date());

    @Test
    void testDiseaseOBOConverter() throws IOException, FrameMergeException {
        DiseaseOBOMessageConverter converter = new DiseaseOBOMessageConverter();

        String headerString =
                "format-version: 1.2\n"
                        + "date: "
                        + NOW
                        + "\n"
                        + "default-namespace: uniprot:diseases";

        String termString1 =
                "[Term]\n"
                        + "id: DI-04860\n"
                        + "name: ZTTK syndrome\n"
                        + "def: \"An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.\" []\n"
                        + "synonym: \"Zhu-Tokita-Takenouchi-Kim syndrome\" [UniProt]\n"
                        + "synonym: \"ZTTK multiple congenital anomalies-mental retardation syndrome\" [UniProt]\n"
                        + "xref: MedGen:CN238690\n"
                        + "xref: MeSH:D000015\n"
                        + "xref: MeSH:D008607\n"
                        + "xref: MIM:617140 \"phenotype\"";

        String term2String =
                "[Term]\n"
                        + "id: DI-03673\n"
                        + "name: 2-aminoadipic 2-oxoadipic aciduria\n"
                        + "def: \"A metabolic disorder characterized by increased levels of 2-oxoadipate and 2-hydroxyadipate in the urine, and elevated 2-aminoadipate in the plasma. Patients can have mild to severe intellectual disability, muscular hypotonia, developmental delay, ataxia, and epilepsy. Most cases are asymptomatic.\" []\n"
                        + "xref: MedGen:C1859817\n"
                        + "xref: MeSH:D000592\n"
                        + "xref: MIM:204750 \"phenotype\"";
        // create a disease object
        DiseaseBuilder diseaseBuilder = new DiseaseBuilder();
        CrossReference xref1 =
                new CrossReference("MIM", "617140", Collections.singletonList("phenotype"));
        CrossReference xref2 = new CrossReference("MedGen", "CN238690");
        CrossReference xref3 = new CrossReference("MeSH", "D000015");
        CrossReference xref4 = new CrossReference("MeSH", "D008607");
        Disease diseaseEntry1 =
                diseaseBuilder
                        .id("ZTTK syndrome")
                        .accession("DI-04860")
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNamesSet(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferencesSet(Arrays.asList(xref1, xref2, xref3, xref4))
                        .build();

        // create another disease object
        diseaseBuilder = new DiseaseBuilder();
        CrossReference xref11 =
                new CrossReference("MIM", "204750", Collections.singletonList("phenotype"));
        CrossReference xref22 = new CrossReference("MedGen", "C1859817");
        CrossReference xref33 = new CrossReference("MeSH", "D000592");
        Disease diseaseEntry2 =
                diseaseBuilder
                        .id("2-aminoadipic 2-oxoadipic aciduria")
                        .accession("DI-03673")
                        .definition(
                                "A metabolic disorder characterized by increased levels of 2-oxoadipate and 2-hydroxyadipate in the urine, and elevated 2-aminoadipate in the plasma. Patients can have mild to severe intellectual disability, muscular hypotonia, developmental delay, ataxia, and epilepsy. Most cases are asymptomatic.")
                        .crossReferencesSet(Arrays.asList(xref11, xref22, xref33))
                        .build();

        OBODoc oboDoc = new OBODoc();
        oboDoc.setHeaderFrame(converter.getHeaderFrame());
        oboDoc.addTermFrame(converter.getTermFrame(diseaseEntry1));
        oboDoc.addTermFrame(converter.getTermFrame(diseaseEntry2));
        String oboDocStr = renderOboToString(oboDoc);
        Assertions.assertTrue(oboDocStr.contains(termString1));
        Assertions.assertTrue(oboDocStr.contains(term2String));
        Assertions.assertTrue(oboDocStr.contains(headerString));
    }

    static String renderOboToString(OBODoc oboDoc) throws IOException {
        OBOFormatWriter writer = new OBOFormatWriter();
        writer.setCheckStructure(true);
        StringWriter out = new StringWriter();
        writer.write(oboDoc, new PrintWriter(out));
        return out.getBuffer().toString();
    }
}
