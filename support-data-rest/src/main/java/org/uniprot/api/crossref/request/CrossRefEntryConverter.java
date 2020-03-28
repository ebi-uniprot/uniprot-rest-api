package org.uniprot.api.crossref.request;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.core.cv.xdb.impl.CrossRefEntryBuilder;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@Slf4j
@Component
public class CrossRefEntryConverter implements Function<CrossRefDocument, CrossRefEntry> {
    @Override
    public CrossRefEntry apply(CrossRefDocument crossRefDocument) {
        CrossRefEntryBuilder builder = new CrossRefEntryBuilder();
        return builder.accession(crossRefDocument.getAccession())
                .abbrev(crossRefDocument.getAbbrev())
                .name(crossRefDocument.getName())
                .pubMedId(crossRefDocument.getPubMedId())
                .doiId(crossRefDocument.getDoiId())
                .linkType(crossRefDocument.getLinkType())
                .server(crossRefDocument.getServer())
                .dbUrl(crossRefDocument.getDbUrl())
                .category(crossRefDocument.getCategory())
                .reviewedProteinCount(crossRefDocument.getReviewedProteinCount())
                .unreviewedProteinCount(crossRefDocument.getUnreviewedProteinCount())
                .build();
    }
}
