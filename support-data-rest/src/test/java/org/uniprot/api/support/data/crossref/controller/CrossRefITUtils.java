package org.uniprot.api.support.data.crossref.controller;

import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.core.cv.xdb.impl.CrossRefEntryBuilder;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

/**
 * @author sahmad
 * @created 01/02/2021
 */
public class CrossRefITUtils {
    public static CrossRefDocument createSolrDocument(String accession, long suffix) {
        CrossRefEntryBuilder entryBuilder = new CrossRefEntryBuilder();
        CrossRefEntry crossRefEntry =
                entryBuilder
                        .id(accession)
                        .abbrev("TIGRFAMs" + suffix)
                        .name("TIGRFAMs; a protein family database" + suffix)
                        .pubMedId("17151080" + suffix)
                        .doiId("10.1093/nar/gkl1043" + suffix)
                        .linkType("Explicit" + suffix)
                        .server("http://tigrfams.jcvi.org/cgi-bin/index.cgi" + suffix)
                        .dbUrl("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s" + suffix)
                        .category("Family and domain databases" + suffix)
                        .reviewedProteinCount(10L + suffix)
                        .unreviewedProteinCount(5L + suffix)
                        .build();

        CrossRefDocument document =
                CrossRefDocument.builder()
                        .id(crossRefEntry.getId())
                        .abbrev(crossRefEntry.getAbbrev())
                        .name(crossRefEntry.getName())
                        .pubMedId(crossRefEntry.getPubMedId())
                        .doiId(crossRefEntry.getDoiId())
                        .linkType(crossRefEntry.getLinkType())
                        .server(crossRefEntry.getServer())
                        .dbUrl(crossRefEntry.getDbUrl())
                        .category(crossRefEntry.getCategory())
                        .reviewedProteinCount(crossRefEntry.getReviewedProteinCount())
                        .unreviewedProteinCount(crossRefEntry.getUnreviewedProteinCount())
                        .build();

        return document;
    }
}
