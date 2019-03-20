package uk.ac.ebi.uniprot.view.api.model;

import lombok.Data;

@Data
public class PageInformation {
    private int resultsPerPage;

    private int currentPage;

    private int totalRecords;
}
