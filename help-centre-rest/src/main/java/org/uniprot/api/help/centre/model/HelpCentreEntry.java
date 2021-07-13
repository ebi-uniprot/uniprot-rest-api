package org.uniprot.api.help.centre.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class HelpCentreEntry {

    private final String id;

    private final String title;

    private final String content;

    @Singular private final List<String> categories;

    @Singular private final Map<String, List<String>> matches;
}
