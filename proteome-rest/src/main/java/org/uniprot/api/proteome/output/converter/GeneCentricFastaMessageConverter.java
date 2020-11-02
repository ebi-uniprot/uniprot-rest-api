package org.uniprot.api.proteome.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.fasta.UniProtKBFasta;
import org.uniprot.core.fasta.impl.UniProtKBFastaBuilder;
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.core.genecentric.Protein;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 23/10/2020
 */
public class GeneCentricFastaMessageConverter
        extends AbstractEntityHttpMessageConverter<GeneCentricEntry> {
    public GeneCentricFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, GeneCentricEntry.class);
    }

    @Override
    protected void writeEntity(GeneCentricEntry entity, OutputStream outputStream)
            throws IOException {
        outputStream.write(getProteinFasta(entity.getCanonicalProtein()));
        if (Utils.notNullNotEmpty(entity.getRelatedProteins())) {
            for (Protein protein : entity.getRelatedProteins()) {
                outputStream.write(getProteinFasta(protein));
            }
        }
    }

    private byte[] getProteinFasta(Protein protein) {
        UniProtKBFasta related = UniProtKBFastaBuilder.from(protein).build();
        return (UniProtKBFastaParser.toFasta(related) + "\n").getBytes();
    }
}
