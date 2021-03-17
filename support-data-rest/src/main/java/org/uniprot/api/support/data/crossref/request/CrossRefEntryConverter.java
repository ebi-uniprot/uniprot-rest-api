package org.uniprot.api.support.data.crossref.request;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.core.Statistics;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.core.cv.xdb.impl.CrossRefEntryBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@Slf4j
@Component
public class CrossRefEntryConverter implements Function<CrossRefDocument, CrossRefEntry> {
    @Override
    public CrossRefEntry apply(CrossRefDocument crossRefDocument) {
        CrossRefEntryBuilder builder = new CrossRefEntryBuilder();
        return builder.id(crossRefDocument.getId())
                .abbrev(crossRefDocument.getAbbrev())
                .name(crossRefDocument.getName())
                .pubMedId(crossRefDocument.getPubMedId())
                .doiId(crossRefDocument.getDoiId())
                .linkType(crossRefDocument.getLinkType())
                .server(crossRefDocument.getServer())
                .dbUrl(crossRefDocument.getDbUrl())
                .category(crossRefDocument.getCategory())
                .statistics(getStatistics(crossRefDocument))
                .build();
    }

    private Statistics getStatistics(CrossRefDocument crossRefDocument) {
        StatisticsBuilder statisticsBuilder = new StatisticsBuilder();
        if (Utils.notNull(crossRefDocument.getReviewedProteinCount())) {
            statisticsBuilder.reviewedProteinCount(crossRefDocument.getReviewedProteinCount());
        }
        if (Utils.notNull(crossRefDocument.getUnreviewedProteinCount())) {
            statisticsBuilder.unreviewedProteinCount(crossRefDocument.getUnreviewedProteinCount());
        }
        return statisticsBuilder.build();
    }
}
