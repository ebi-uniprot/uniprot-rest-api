package org.uniprot.api.uniparc.output.converter;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.uniparc.model.UniParcEntryWrapper;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author sahmad
 * @created 24/03/2021
 */
public class UniParcJsonMessageConverter extends AbstractEntityHttpMessageConverter<UniParcEntryWrapper> {
    private JsonMessageConverter<UniParcEntry> jsonMessageConverter;

    public UniParcJsonMessageConverter(Class<UniParcEntryWrapper> messageConverterEntryClass,
                                       ReturnFieldConfig returnFieldConfig) {
        super(MediaType.APPLICATION_JSON, messageConverterEntryClass);
        this.jsonMessageConverter =
                new JsonMessageConverter<>(
                        UniParcJsonConfig.getInstance().getSimpleObjectMapper(),
                        UniParcEntry.class,
                        returnFieldConfig);
    }

    @Override
    protected void before(MessageConverterContext<UniParcEntryWrapper> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        this.jsonMessageConverter.before(buildJsonConverterContext(context), outputStream);
    }

    @Override
    protected void writeEntity(UniParcEntryWrapper entity, OutputStream outputStream) throws IOException {
        this.jsonMessageConverter.writeEntity(entity.getEntry(), outputStream);
    }

    @Override
    protected void cleanUp() {
        this.jsonMessageConverter.cleanUp();
    }

    private MessageConverterContext<UniParcEntry> buildJsonConverterContext(
            MessageConverterContext<UniParcEntryWrapper> context) {
        MessageConverterContext.MessageConverterContextBuilder<UniParcEntry> contextBuilder
                = MessageConverterContext.builder();
        contextBuilder.fields(context.getFields()).entityOnly(context.isEntityOnly());
        contextBuilder.facets(context.getFacets()).matchedFields(context.getMatchedFields());
        contextBuilder.fileType(context.getFileType());
        return contextBuilder.build();
    }
}
