package org.uniprot.api.support.data.disease.response;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseEntryBuilder;

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
        DiseaseEntryBuilder diseaseBuilder = new DiseaseEntryBuilder();
        DiseaseCrossReference xref1 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MIM")
                        .id("617140")
                        .propertiesAdd("phenotype")
                        .build();
        DiseaseCrossReference xref2 =
                new DiseaseCrossReferenceBuilder().databaseType("MedGen").id("CN238690").build();
        DiseaseCrossReference xref3 =
                new DiseaseCrossReferenceBuilder().databaseType("MeSH").id("D000015").build();
        DiseaseCrossReference xref4 =
                new DiseaseCrossReferenceBuilder().databaseType("MeSH").id("D008607").build();
        DiseaseEntry diseaseEntry1 =
                diseaseBuilder
                        .name("ZTTK syndrome")
                        .id("DI-04860")
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNamesSet(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferencesSet(Arrays.asList(xref1, xref2, xref3, xref4))
                        .build();

        // create another disease object
        diseaseBuilder = new DiseaseEntryBuilder();
        DiseaseCrossReference xref11 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MIM")
                        .id("204750")
                        .propertiesAdd("phenotype")
                        .build();
        DiseaseCrossReference xref22 =
                new DiseaseCrossReferenceBuilder().databaseType("MedGen").id("C1859817").build();
        DiseaseCrossReference xref33 =
                new DiseaseCrossReferenceBuilder().databaseType("MeSH").id("D000592").build();
        DiseaseEntry diseaseEntry2 =
                diseaseBuilder
                        .name("2-aminoadipic 2-oxoadipic aciduria")
                        .id("DI-03673")
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
