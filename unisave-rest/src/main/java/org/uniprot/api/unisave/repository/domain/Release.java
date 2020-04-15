package org.uniprot.api.unisave.repository.domain;

import java.util.Date;

/**
 * Represent a release of uniprot.
 *
 * @author wudong
 */
public interface Release {

    /**
     * The release number.
     *
     * @return
     */
    String getReleaseNumber();

    /**
     * The public release date of this release.
     *
     * @return
     */
    Date getReleaseDate();

    /**
     * What database this release belong to.
     *
     * @return
     */
    DatabaseEnum getDatabase();
}
