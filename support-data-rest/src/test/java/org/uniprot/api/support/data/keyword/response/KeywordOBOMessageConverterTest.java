package org.uniprot.api.support.data.keyword.response;

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
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.cv.keyword.KeywordFileReader;

/**
 * @author sahmad
 * @created 22/01/2021
 */
class KeywordOBOMessageConverterTest {
    @Test
    void testTypeStanza() throws IOException {
        // when
        KeywordOBOMessageConverter converter = new KeywordOBOMessageConverter();
        Frame typeStanza = converter.getTypeDefStanza();
        assertNotNull(typeStanza);
        // convert to string
        String typeStanzaStr = convertFrame(typeStanza);
        // verify
        assertNotNull(typeStanzaStr);
        assertEquals(
                "[Typedef]\n" + "id: category\n" + "name: category\n" + "is_cyclic: false\n\n",
                typeStanzaStr);
    }

    @Test
    void testMinTerm() throws IOException {
        KeywordOBOMessageConverter converter = new KeywordOBOMessageConverter();
        List<String> lines = new ArrayList<>();
        lines.add("_______________________________");
        lines.add("IC   Biological process.");
        lines.add("AC   KW-9999");
        lines.add("DE   Keywords assigned to proteins because they are involved in a");
        lines.add("DE   particular biological process.");
        lines.add("//");
        KeywordFileReader reader = new KeywordFileReader();
        List<KeywordEntry> keywordEntries = reader.parseLines(lines);
        assertNotNull(keywordEntries);
        assertEquals(1, keywordEntries.size());
        Frame frame = converter.getTermFrame(keywordEntries.get(0));
        assertNotNull(frame);
        String termFrameStr = convertFrame(frame);
        assertNotNull(termFrameStr);
        assertEquals(
                "[Term]\n"
                        + "id: KW-9999\n"
                        + "name: Biological process\n"
                        + "def: \"Keywords assigned to proteins because they are involved in a particular biological process.\" []\n\n",
                termFrameStr);
    }

    @Test
    void testFullTerm() throws IOException {
        KeywordOBOMessageConverter converter = new KeywordOBOMessageConverter();
        List<String> lines = new ArrayList<>();
        lines.add("_______________________________");
        lines.add("ID   Cobalt.");
        lines.add("AC   KW-0170");
        lines.add("DE   Protein which binds at least one cobalt atom, or protein whose");
        lines.add("DE   function is cobalt-dependent. Cobalt is a metallic element, chemical");
        lines.add("DE   symbol Co.");
        lines.add("SY   Co; Cobalt cation; Cobalt ion; Co cation; Co ion.");
        lines.add("HI   Ligand: Cobalt.");
        lines.add("WW   https://www.webelements.com/cobalt/");
        lines.add("CA   Ligand.");
        lines.add("//");
        KeywordFileReader reader = new KeywordFileReader();
        List<KeywordEntry> keywordEntries = reader.parseLines(lines);
        assertNotNull(keywordEntries);
        assertEquals(1, keywordEntries.size());
        Frame frame = converter.getTermFrame(keywordEntries.get(0));
        assertNotNull(frame);
        String termFrameStr = convertFrame(frame);
        assertNotNull(termFrameStr);
        assertEquals(
                "[Term]\n"
                        + "id: KW-0170\n"
                        + "name: Cobalt\n"
                        + "def: \"Protein which binds at least one cobalt atom, or protein whose function is cobalt-dependent. Cobalt is a metallic element, chemical symbol Co.\" []\n"
                        + "synonym: \"Co\" [UniProt]\n"
                        + "synonym: \"Co cation\" [UniProt]\n"
                        + "synonym: \"Co ion\" [UniProt]\n"
                        + "synonym: \"Cobalt cation\" [UniProt]\n"
                        + "synonym: \"Cobalt ion\" [UniProt]\n"
                        + "xref: https://www.webelements.com/cobalt/\n\n",
                termFrameStr);
    }

    private String convertFrame(Frame frame) throws IOException {
        OBOFormatWriter oboFormatWriter = new OBOFormatWriter();
        StringWriter out = new StringWriter();
        oboFormatWriter.write(frame, new PrintWriter(out), null);
        return out.getBuffer().toString();
    }
}
