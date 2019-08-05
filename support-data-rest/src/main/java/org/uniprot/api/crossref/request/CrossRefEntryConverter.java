package org.uniprot.api.crossref.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.uniprot.core.crossref.CrossRefEntry;
import org.uniprot.core.crossref.CrossRefEntryBuilder;
import org.uniprot.core.crossref.CrossRefEntryImpl;
import org.uniprot.core.json.parser.crossref.CrossRefJsonConfig;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.function.Function;

@Slf4j
@Service
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
