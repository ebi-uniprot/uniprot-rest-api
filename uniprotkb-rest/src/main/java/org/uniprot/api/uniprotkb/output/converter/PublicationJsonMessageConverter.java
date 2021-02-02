package org.uniprot.api.uniprotkb.output.converter;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.json.parser.publication.MappedPublicationsJsonConfig;

/**
 * @author lgonzales
 * @since 2019-12-13
 */
public class PublicationJsonMessageConverter extends JsonMessageConverter<PublicationEntry> {
    public PublicationJsonMessageConverter() {
        super(
                MappedPublicationsJsonConfig.getInstance().getSimpleObjectMapper(),
                PublicationEntry.class,
                null);
    }
}
