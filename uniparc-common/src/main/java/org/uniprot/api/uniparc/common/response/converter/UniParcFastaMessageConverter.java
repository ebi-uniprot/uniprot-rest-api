package org.uniprot.api.uniparc.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.UniParcFastaParser;
import org.uniprot.core.parser.fasta.uniparc.UniParcProteomeFastaParser;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.util.Utils;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
public class UniParcFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniParcEntry> {

    private static final ThreadLocal<Boolean> TL_ENTITY_ONLY = new ThreadLocal<>();

    public UniParcFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntry.class);
    }

    public UniParcFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntry.class, downloadGatekeeper);
    }

    @Override
    protected void before(
            MessageConverterContext<UniParcEntry> context, OutputStream outputStream) {
        TL_ENTITY_ONLY.set(context.isEntityOnly());
    }

    @Override
    protected void writeEntity(UniParcEntry entity, OutputStream outputStream) throws IOException {
        Boolean entityOnly = TL_ENTITY_ONLY.get();
        if (Utils.notNull(entityOnly) && entityOnly) {
            outputStream.write((UniParcFastaParser.toFasta(entity) + "\n").getBytes());
        } else {
            outputStream.write((UniParcProteomeFastaParser.toFasta(entity) + "\n").getBytes());
        }
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        TL_ENTITY_ONLY.remove();
    }
}
