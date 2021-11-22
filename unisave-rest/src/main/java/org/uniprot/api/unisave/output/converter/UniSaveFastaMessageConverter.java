package org.uniprot.api.unisave.output.converter;

import static org.uniprot.api.unisave.service.impl.UniSaveServiceImpl.AGGREGATED_SEQUENCE_MEMBER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import lombok.extern.slf4j.Slf4j;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.StopStreamException;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.core.flatfile.parser.UniProtParser;
import org.uniprot.core.flatfile.parser.impl.DefaultUniProtParser;
import org.uniprot.core.flatfile.parser.impl.SupportingDataMapImpl;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/** @author Edd */
@Slf4j
public class UniSaveFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniSaveEntry> {
    private static final int MAX_COLUMNS = 60;
    private static final UniProtParser UNIPROT_FF_PARSER =
            new DefaultUniProtParser(new SupportingDataMapImpl(), true);

    public UniSaveFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniSaveEntry.class);
    }

    @Override
    protected void writeEntity(UniSaveEntry entity, OutputStream outputStream) throws IOException {
        String fastaContent;
        String content = entity.getContent();

        if (content.startsWith(AGGREGATED_SEQUENCE_MEMBER)) {
            String sequence = extractSequenceFromContent(content);

            StringBuilder fastaStringBuilder = new StringBuilder();
            addUniqueSequenceFastaHeader(entity, fastaStringBuilder);
            addSequence(sequence, fastaStringBuilder);
            fastaContent = fastaStringBuilder.toString();
        } else if (entity.isCurrentRelease()) {
            UniProtKBEntry uniProtKBEntry = UNIPROT_FF_PARSER.parse(content);
            fastaContent = UniProtKBFastaParser.toFasta(uniProtKBEntry);
        } else {
            String sequence = extractSequenceFromContent(content);

            StringBuilder fastaStringBuilder = new StringBuilder();
            addStandardFastaHeader(entity, fastaStringBuilder);
            addSequence(sequence, fastaStringBuilder);
            fastaContent = fastaStringBuilder.toString();
        }

        // add a newline if one isn't present
        if (!fastaContent.endsWith("\n")) {
            fastaContent += "\n";
        }

        outputStream.write(fastaContent.getBytes());
    }

    private String extractSequenceFromContent(String content) {
        StringBuilder sequenceStringBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new StringReader(content));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("SQ")) {
                    String sequenceLine;
                    while ((sequenceLine = br.readLine()) != null && !sequenceLine.equals("//")) {
                        addSequenceWithoutSpaces(sequenceStringBuilder, sequenceLine);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error parsing entry content", e);
            throw new StopStreamException("Error parsing entry content", e);
        }

        return sequenceStringBuilder.toString();
    }

    private void addSequence(String sequence, StringBuilder fastaStringBuilder) {
        int columnCounter = 0;
        for (char seqChar : sequence.toCharArray()) {
            fastaStringBuilder.append(seqChar);
            if ((++columnCounter % MAX_COLUMNS == 0) && (columnCounter < sequence.length())) {
                fastaStringBuilder.append("\n");
            }
        }
    }

    private void addUniqueSequenceFastaHeader(
            UniSaveEntry entity, StringBuilder fastaStringBuilder) {
        String entryVersion;
        if (entity.getEntryVersion().equals(entity.getEntryVersionUpper())) {
            entryVersion = entity.getEntryVersion().toString();
        } else {
            entryVersion =
                    entity.getEntryVersion().toString()
                            + "-"
                            + entity.getEntryVersionUpper().toString();
        }
        fastaStringBuilder
                .append('>')
                .append(entity.getAccession())
                .append(": EV=")
                .append(entryVersion)
                .append(" SV=")
                .append(entity.getSequenceVersion())
                .append('\n');
    }

    private void addStandardFastaHeader(UniSaveEntry entity, StringBuilder fastaStringBuilder) {
        fastaStringBuilder
                .append('>')
                .append(entity.getDatabase().equalsIgnoreCase("swiss-prot") ? "sp" : "tr")
                .append('|')
                .append(entity.getAccession())
                .append('|')
                .append("Release ")
                .append(entity.getFirstRelease())
                .append('|')
                .append(entity.getFirstReleaseDate())
                .append('\n');
    }

    private void addSequenceWithoutSpaces(
            StringBuilder sequenceStringBuilder, String sequenceLine) {
        for (char seqChar : sequenceLine.trim().toCharArray()) {
            // remove spaces from within sequence line
            if (seqChar != ' ') {
                sequenceStringBuilder.append(seqChar);
            }
        }
    }
}
