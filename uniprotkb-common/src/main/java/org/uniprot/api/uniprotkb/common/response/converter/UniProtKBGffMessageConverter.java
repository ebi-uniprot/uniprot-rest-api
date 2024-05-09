package org.uniprot.api.uniprotkb.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.gff.uniprot.UniProtGffParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBGffMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntry> {

    public static final String GFF_HEADER = "##gff-version 3";

    public UniProtKBGffMessageConverter() {
        super(UniProtMediaType.GFF_MEDIA_TYPE, UniProtKBEntry.class);
    }

    public UniProtKBGffMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.GFF_MEDIA_TYPE, UniProtKBEntry.class, downloadGatekeeper);
    }

    @Override
    protected void before(
            MessageConverterContext<UniProtKBEntry> context, OutputStream outputStream)
            throws IOException {
        outputStream.write((GFF_HEADER + "\n").getBytes());
    }

    @Override
    protected void writeEntity(UniProtKBEntry entity, OutputStream outputStream)
            throws IOException {
        if (entity.isActive()) {
            outputStream.write((UniProtGffParser.convert(entity) + "\n").getBytes());
        }
    }
}
