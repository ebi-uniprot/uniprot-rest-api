package org.uniprot.api.help.centre.output;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
public class HelpCentreMarkdownMessageConverter
        extends AbstractEntityHttpMessageConverter<HelpCentreEntry> {

    public HelpCentreMarkdownMessageConverter() {
        super(UniProtMediaType.MARKDOWN_MEDIA_TYPE, HelpCentreEntry.class);
    }

    @Override
    protected void writeEntity(HelpCentreEntry entity, OutputStream outputStream)
            throws IOException {
        outputStream.write("---".getBytes());
        outputStream.write("\n".getBytes());
        outputStream.write(("title: " + entity.getTitle()).getBytes());
        outputStream.write("\n".getBytes());
        outputStream.write(("categories: " + String.join(",", entity.getCategories())).getBytes());
        outputStream.write("\n".getBytes());
        outputStream.write("---".getBytes());
        outputStream.write("\n".getBytes());
        outputStream.write((entity.getContent()).getBytes());
    }
}
