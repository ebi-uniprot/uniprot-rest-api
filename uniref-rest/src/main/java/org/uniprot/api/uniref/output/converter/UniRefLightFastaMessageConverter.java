package org.uniprot.api.uniref.output.converter;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.UniRefFastaParser;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
public class UniRefLightFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniRefEntryLight> {

    public UniRefLightFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniRefEntryLight.class);
    }

    @Override
    protected void writeEntity(UniRefEntryLight entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniRefFastaParser.toFasta(entity) + "\n").getBytes());
    }
}
