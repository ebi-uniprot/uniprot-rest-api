package org.uniprot.api.common.repository.search.term;

import lombok.Builder;
import lombok.Getter;

/**
 * A class representing meta-data about a term that has one or more hits an index. The {@code url}
 * attribute is used to link to a resource that displays more information about this term.
 *
 * <p>Note that often the term {@code name}s will mapto 1-to-1 to fields in the search index; but it
 * need not mapto to a specific field at all / may mapto to multiple fields.
 *
 * <p>Created 12/06/19
 *
 * @author Edd
 */
@Builder
@Getter
public class TermInfo {
    private String name;
    private long hits;
}
