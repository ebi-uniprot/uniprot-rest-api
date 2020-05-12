package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class Interaction {
    private List<IntActComment> interactions;

    @Getter
    @Builder
    public static class IntActComment {
        private String accession1;
        private String accession2;
        private String chain1;
        private String chain2;
        private String gene;
        private String interactor1;
        private String interactor2;
        private boolean organismDiffer;
        private int experiments;
    }
}
