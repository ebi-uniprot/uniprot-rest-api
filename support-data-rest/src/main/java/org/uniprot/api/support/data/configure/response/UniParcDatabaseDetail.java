package org.uniprot.api.support.data.configure.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author lgonzales
 * @since 09/02/2021
 */
@Builder
@Getter
@AllArgsConstructor
public class UniParcDatabaseDetail {

    private final String name;

    private final String displayName;

    private final boolean alive;

    private final String uriLink;
}
