package org.uniprot.api.unisave.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Edd
 */
@Slf4j
public class UniSaveTSVMessageConverter extends AbstractEntityHttpMessageConverter<UniSaveEntry> {
    private static final String HEADER =
            "Entry version\tSequence version\tEntry name\tDatabase\tNumber\tDate\tReplaces\tReplaced by\n";

    public UniSaveTSVMessageConverter() {
        super(UniProtMediaType.TSV_MEDIA_TYPE, UniSaveEntry.class);
    }

    @Override
    protected void before(MessageConverterContext<UniSaveEntry> context, OutputStream outputStream)
            throws IOException {
        outputStream.write(HEADER.getBytes());
    }

    @Override
    protected void writeEntity(UniSaveEntry entity, OutputStream outputStream) throws IOException {
        String record =
                String.valueOf(entity.getEntryVersion())
                        + '\t'
                        + entity.getSequenceVersion()
                        + '\t'
                        + entity.getName()
                        + '\t'
                        + entity.getDatabase()
                        + '\t'
                        + entity.getLastRelease()
                        + '\t'
                        + entity.getLastReleaseDate()
                        + '\t'
                        + getReplaces(entity)
                        + '\t'
                        + getReplacedBy(entity)
                        + '\n';

        outputStream.write(record.getBytes());
    }

    private String getReplaces(UniSaveEntry entity) {
        List<String> events = entity.getReplacingAcc();
        if (Utils.notNullNotEmpty(events)) {
            return events.get(events.size() - 1);
        }
        return "";
    }

    private String getReplacedBy(UniSaveEntry entity) {
        List<String> events = entity.getMergedTo();
        if (Utils.notNullNotEmpty(events)) {
            return events.get(events.size() - 1);
        }
        return "";
    }
}
