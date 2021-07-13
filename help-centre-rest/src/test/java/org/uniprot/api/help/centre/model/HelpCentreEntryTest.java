package org.uniprot.api.help.centre.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 * @since 13/07/2021
 */
class HelpCentreEntryTest {

    @Test
    void canCreateHelpCentreEntry() {
        List<String> categories = List.of("category");
        Map<String, List<String>> matches = Map.of("title", List.of("values"));
        HelpCentreEntry entry =
                HelpCentreEntry.builder()
                        .id("id")
                        .title("title")
                        .content("content")
                        .categories(categories)
                        .matches(matches)
                        .build();

        assertNotNull(entry);
        assertEquals("id", entry.getId());
        assertEquals("title", entry.getTitle());
        assertEquals("content", entry.getContent());
        assertEquals(categories, entry.getCategories());
        assertEquals(matches, entry.getMatches());
    }
}
