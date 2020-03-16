package org.uniprot.api.uniprotkb.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.uniprot.core.flatfile.parser.UniprotkbLineParser;
import org.uniprot.core.flatfile.parser.impl.DefaultUniprotkbLineParserFactory;
import org.uniprot.core.flatfile.parser.impl.ac.AcLineObject;
import org.uniprot.core.flatfile.parser.impl.cc.cclineobject.CcLineObject;
import org.uniprot.core.flatfile.parser.impl.de.DeLineObject;
import org.uniprot.core.flatfile.parser.impl.dr.DrLineObject;
import org.uniprot.core.flatfile.parser.impl.dt.DtLineObject;
import org.uniprot.core.flatfile.parser.impl.entry.EntryObject;
import org.uniprot.core.flatfile.parser.impl.entry.EntryObjectConverter;
import org.uniprot.core.flatfile.parser.impl.ft.FtLineObject;
import org.uniprot.core.flatfile.parser.impl.gn.GnLineObject;
import org.uniprot.core.flatfile.parser.impl.id.IdLineObject;
import org.uniprot.core.flatfile.parser.impl.kw.KwLineObject;
import org.uniprot.core.flatfile.parser.impl.oc.OcLineObject;
import org.uniprot.core.flatfile.parser.impl.og.OgLineObject;
import org.uniprot.core.flatfile.parser.impl.oh.OhLineObject;
import org.uniprot.core.flatfile.parser.impl.os.OsLineObject;
import org.uniprot.core.flatfile.parser.impl.ox.OxLineObject;
import org.uniprot.core.flatfile.parser.impl.pe.PeLineObject;
import org.uniprot.core.flatfile.parser.impl.sq.SqLineObject;
import org.uniprot.core.flatfile.writer.LineType;
import org.uniprot.core.uniprotkb.UniProtkbEntry;

class UniProtEntryObjectProxy {
    private EntryObject entryObject;
    private final UniprotkbLineParser<EntryObject> entryParser;

    private UniProtEntryObjectProxy() {
        this.entryParser = new DefaultUniprotkbLineParserFactory().createEntryParser();
    }

    private static UniProtEntryObjectProxy createEntryFromString(String entryText) {
        UniProtEntryObjectProxy uniProtEntryObject = new UniProtEntryObjectProxy();
        uniProtEntryObject.entryObject = uniProtEntryObject.entryParser.parse(entryText);

        return uniProtEntryObject;
    }

    static UniProtEntryObjectProxy createEntryFromInputStream(InputStream stream)
            throws IOException {
        StringBuilder entryText = new StringBuilder();

        try (InputStreamReader ir = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(ir)) {
            String line;

            while ((line = br.readLine()) != null) {
                entryText.append(line).append("\n");
            }
        }

        return createEntryFromString(entryText.toString());
    }

    void updateEntryObject(LineType lineType, String replacement) {
        DefaultUniprotkbLineParserFactory parserFactory = new DefaultUniprotkbLineParserFactory();

        if (!replacement.endsWith("\n")) {
            replacement += "\n";
        }

        switch (lineType) {
            case AC:
                UniprotkbLineParser<AcLineObject> acLineParser = parserFactory.createAcLineParser();
                entryObject.ac = acLineParser.parse(replacement);
                break;
            case DE:
                UniprotkbLineParser<DeLineObject> deLineParser = parserFactory.createDeLineParser();
                entryObject.de = deLineParser.parse(replacement);
                break;
            case DR:
                UniprotkbLineParser<DrLineObject> drLineParser = parserFactory.createDrLineParser();
                entryObject.dr = drLineParser.parse(replacement);
                break;
            case DT:
                UniprotkbLineParser<DtLineObject> dtLineParser = parserFactory.createDtLineParser();
                entryObject.dt = dtLineParser.parse(replacement);
                break;
            case GN:
                UniprotkbLineParser<GnLineObject> gnLineParser = parserFactory.createGnLineParser();
                entryObject.gn = gnLineParser.parse(replacement);
                break;
            case ID:
                UniprotkbLineParser<IdLineObject> idLineParser = parserFactory.createIdLineParser();
                entryObject.id = idLineParser.parse(replacement);
                break;
            case KW:
                UniprotkbLineParser<KwLineObject> kwLineParser = parserFactory.createKwLineParser();
                entryObject.kw = kwLineParser.parse(replacement);
                break;
            case OC:
                UniprotkbLineParser<OcLineObject> ocLineParser = parserFactory.createOcLineParser();
                entryObject.oc = ocLineParser.parse(replacement);
                break;
            case OG:
                UniprotkbLineParser<OgLineObject> ogLineParser = parserFactory.createOgLineParser();
                entryObject.og = ogLineParser.parse(replacement);
                break;
            case OH:
                UniprotkbLineParser<OhLineObject> ohLineParser = parserFactory.createOhLineParser();
                entryObject.oh = ohLineParser.parse(replacement);
                break;
            case OS:
                UniprotkbLineParser<OsLineObject> osLineParser = parserFactory.createOsLineParser();
                entryObject.os = osLineParser.parse(replacement);
                break;
            case OX:
                UniprotkbLineParser<OxLineObject> oxLineParser = parserFactory.createOxLineParser();
                entryObject.ox = oxLineParser.parse(replacement);
                break;
            case CC:
                UniprotkbLineParser<CcLineObject> ccLineParser = parserFactory.createCcLineParser();
                entryObject.cc = ccLineParser.parse(replacement);
                break;
            case FT:
                UniprotkbLineParser<FtLineObject> ftLineParser = parserFactory.createFtLineParser();
                entryObject.ft = ftLineParser.parse(replacement);
                break;
            case PE:
                UniprotkbLineParser<PeLineObject> peLineParser = parserFactory.createPeLineParser();
                entryObject.pe = peLineParser.parse(replacement);
                break;
            case SQ:
                UniprotkbLineParser<SqLineObject> sqLineParser = parserFactory.createSqLineParser();
                entryObject.sq = sqLineParser.parse(replacement);
                break;
            default:
                throw new IllegalArgumentException(
                        "Line type to update not implemented: " + lineType);
        }
    }

    UniProtkbEntry convertToUniProtEntry(EntryObjectConverter entryObjectConverter) {
        return entryObjectConverter.convert(this.entryObject);
    }
}
