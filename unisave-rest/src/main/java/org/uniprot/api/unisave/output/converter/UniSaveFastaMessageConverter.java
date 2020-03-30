package org.uniprot.api.unisave.output.converter;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

/** @author Edd */
public class UniSaveFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniSaveEntry> {

    public UniSaveFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniSaveEntry.class);
    }

    @Override
    protected void writeEntity(UniSaveEntry entity, OutputStream outputStream) throws IOException {
        StringBuilder sequenceStringBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new StringReader(entity.getContent()));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("SQ")) {
                    String sequenceLine;
                    while ((sequenceLine = br.readLine()) != null && !sequenceLine.equals("//")) {
                        sequenceStringBuilder.append(sequenceLine.trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error in parse the content", e);
        }
        String sequence = sequenceStringBuilder.toString();

        StringBuilder fastaStringBuilder = new StringBuilder();
        fastaStringBuilder
                .append('>')
                .append(entity.getDatabase())
                .append('|')
                .append(entity.getAccession())
                .append('|')
                .append("Release ")
                .append(entity.getFirstRelease())
                .append('|')
                .append(entity.getFirstReleaseDate())
                .append('\n');
        for (int i = 1; i < sequence.length(); i++) {
            fastaStringBuilder.append(sequence.charAt(i - 1));
            if (i % fastaStringBuilder.length() == 0) {
                fastaStringBuilder.append('\n');
            }
        }
        fastaStringBuilder.append('\n');
        outputStream.write(fastaStringBuilder.toString().getBytes());
    }
}
