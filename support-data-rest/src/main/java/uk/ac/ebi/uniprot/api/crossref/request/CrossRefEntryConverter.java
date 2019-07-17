package uk.ac.ebi.uniprot.api.crossref.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntryBuilder;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntryImpl;
import uk.ac.ebi.uniprot.json.parser.crossref.CrossRefJsonConfig;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;

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
