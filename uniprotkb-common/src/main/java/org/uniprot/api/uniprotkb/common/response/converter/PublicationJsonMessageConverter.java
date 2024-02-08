package org.uniprot.api.uniprotkb.common.response.converter;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.uniprotkb.common.repository.model.PublicationEntry;
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

    public PublicationJsonMessageConverter(Gatekeeper downloadGatekeeper) {
        super(
                MappedPublicationsJsonConfig.getInstance().getSimpleObjectMapper(),
                PublicationEntry.class,
                null,
                downloadGatekeeper);
    }
}
