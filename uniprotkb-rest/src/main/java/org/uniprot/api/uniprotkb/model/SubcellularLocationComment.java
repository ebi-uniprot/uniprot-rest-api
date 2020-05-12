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
public class SubcellularLocationComment {
    private final List<SubcellularLocation> locations;
    private final List<EvidencedString> text;

    @Getter
    @Builder
    public static class SubcellularLocation {
        private EvidencedString location;
        private EvidencedString topology;
        private EvidencedString orientation;
    }
}
