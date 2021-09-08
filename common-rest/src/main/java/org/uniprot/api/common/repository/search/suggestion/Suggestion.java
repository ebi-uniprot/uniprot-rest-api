package org.uniprot.api.common.repository.search.suggestion;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * The purpose of this class is to represent a suggestion returned from a Solr spellchecker entity.
 * For example, if an index contains "bell", but a user queries, "ball", then Solr can suggest to
 * the user to search for "bell", because it is similar to "ball".
 *
 * <p>A suggestion contains the suggested term, and the number of hits this term has within the
 * index.
 *
 * <p>Created 28/07/2021
 *
 * @author Edd
 */
@Data
@Builder
public class Suggestion {
    private String original;
    @Singular private List<Alternative> alternatives;
}
