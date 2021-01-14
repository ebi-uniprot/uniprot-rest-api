package org.uniprot.api.support.data.subcellular.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.cv.subcell.SubcellularLocationFileReader;

/**
 * @author lgonzales
 * @since 2019-08-29
 */
class SubcellularLocationOBOMessageConverterTest {

    @Test
    void canParseRequiredOBOFields() throws IOException {
        // when
        List<String> sampleLines = new ArrayList<>();
        sampleLines.add("_______________________________");
        sampleLines.add("ID   Acidocalcisome.");
        sampleLines.add("AC   SL-0002");
        sampleLines.add("DE   The acidocalcisome");
        sampleLines.add("//");

        SubcellularLocationOBOMessageConverter converter =
                new SubcellularLocationOBOMessageConverter();
        OBOFormatWriter oboFormatWriter = new OBOFormatWriter();

        SubcellularLocationFileReader subcellularLocationFileReader =
                new SubcellularLocationFileReader();
        List<SubcellularLocationEntry> entries =
                subcellularLocationFileReader.parseLines(sampleLines);
        assertNotNull(entries);
        assertEquals(1, entries.size());

        // do
        Frame frame = converter.getTermFrame(entries.get(0));
        assertNotNull(frame);

        StringWriter out = new StringWriter();
        oboFormatWriter.write(frame, new PrintWriter(out), null);
        String oboFormatStr = out.getBuffer().toString();

        // check

        String expectedOutput =
                "[Term]\n"
                        + "id: SL-0002\n"
                        + "name: Acidocalcisome\n"
                        + "namespace: uniprot:locations:cellular_component\n"
                        + "def: \"The acidocalcisome\" []\n\n";
        assertNotNull(oboFormatStr);
        assertEquals(expectedOutput, oboFormatStr);
    }

    @Test
    void canParseAllOBOFields() throws IOException {
        // when
        List<String> sampleLines = new ArrayList<>();
        sampleLines.add("_______________________________");
        sampleLines.add("ID   Acidocalcisome.");
        sampleLines.add("AC   SL-0002");
        sampleLines.add("DE   The acidocalcisome");
        sampleLines.add("SY   Acidocalcisome lumen.");
        sampleLines.add("GO   GO:0002081; outer acrosomal membrane");
        sampleLines.add("WW   http://www.cf.ac.uk/biosi/staff/ehrmann/tools/ecce/ecce.htm");
        sampleLines.add("HP   Acidocalcisome.");
        sampleLines.add("HI   Acidocalcisome.");
        sampleLines.add("//");

        SubcellularLocationOBOMessageConverter converter =
                new SubcellularLocationOBOMessageConverter();
        OBOFormatWriter oboFormatWriter = new OBOFormatWriter();

        SubcellularLocationFileReader subcellularLocationFileReader =
                new SubcellularLocationFileReader();
        List<SubcellularLocationEntry> entries =
                subcellularLocationFileReader.parseLines(sampleLines);
        assertNotNull(entries);
        assertEquals(1, entries.size());

        // do
        Frame frame = converter.getTermFrame(entries.get(0));
        assertNotNull(frame);

        StringWriter out = new StringWriter();
        oboFormatWriter.write(frame, new PrintWriter(out), null);
        String oboFormatStr = out.getBuffer().toString();

        // check

        String expectedOutput =
                "[Term]\n"
                        + "id: SL-0002\n"
                        + "name: Acidocalcisome\n"
                        + "namespace: uniprot:locations:cellular_component\n"
                        + "def: \"The acidocalcisome\" []\n"
                        + "synonym: \"Acidocalcisome lumen\" [UniProt]\n"
                        + "xref: GO:0002081 \"outer acrosomal membrane\"\n"
                        + "xref: http://www.cf.ac.uk/biosi/staff/ehrmann/tools/ecce/ecce.htm\n"
                        + "is_a: SL-0002\n"
                        + "relationship: part_of SL-0002\n\n";
        assertNotNull(oboFormatStr);
        assertEquals(expectedOutput, oboFormatStr);
    }
}
