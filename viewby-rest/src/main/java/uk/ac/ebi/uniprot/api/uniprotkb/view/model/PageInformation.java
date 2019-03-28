package uk.ac.ebi.uniprot.api.uniprotkb.view.model;

import lombok.Data;

@Data
public class PageInformation {
    private int resultsPerPage;

    private int currentPage;

    private int totalRecords;
}
