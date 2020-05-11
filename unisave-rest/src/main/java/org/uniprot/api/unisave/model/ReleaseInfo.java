package org.uniprot.api.unisave.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@Data
@Builder
public class ReleaseInfo {
    String releaseNumber;
    String releaseDate;
}
