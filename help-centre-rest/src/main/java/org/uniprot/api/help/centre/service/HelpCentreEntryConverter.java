package org.uniprot.api.help.centre.service;

import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Component
public class HelpCentreEntryConverter implements Function<HelpDocument, HelpCentreEntry> {

    @Override
    public HelpCentreEntry apply(HelpDocument helpDocument) {
        return HelpCentreEntry.builder()
                .id(helpDocument.getId())
                .title(helpDocument.getTitle())
                .content(helpDocument.getContent())
                .categories(helpDocument.getCategories())
                // TODO: matches(??)
                .build();
    }
}
