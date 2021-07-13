package org.uniprot.api.help.centre.service;

import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Component
public class HelpCentreEntryConverter implements Function<HelpDocument, HelpCentreEntry> {

    @Override
    public HelpCentreEntry apply(HelpDocument helpDocument) {
        HelpCentreEntry.HelpCentreEntryBuilder builder = HelpCentreEntry.builder()
                .id(helpDocument.getId())
                .title(helpDocument.getTitle())
                .content(helpDocument.getContent());

        if(Utils.notNullNotEmpty(helpDocument.getCategories())){
            builder.categories(helpDocument.getCategories());
        }

        if(Utils.notNullNotEmpty(helpDocument.getMatches())){
            builder.matches(helpDocument.getMatches());
        }

        return builder.build();
    }
}
