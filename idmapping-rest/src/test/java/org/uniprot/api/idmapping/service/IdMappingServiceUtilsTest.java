package org.uniprot.api.idmapping.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;

class IdMappingServiceUtilsTest {

    @Test
    void getExtraOptionsWithoutValue() {
        IdMappingResult idmappingResult = IdMappingResult.builder().build();
        ExtraOptions result = IdMappingServiceUtils.getExtraOptions(idmappingResult);
        assertNotNull(result);
        assertEquals(result.getFailedIds(), List.of());
        assertEquals(result.getSuggestedIds(), List.of());
        assertNull(result.getObsoleteCount());
    }

    @Test
    void getExtraOptionsWithValues() {
        List<String> failedIds = List.of("id1", "id2");
        List<IdMappingStringPair> suggestIds =
                List.of(getMappingPair("id1"), getMappingPair("id2"));
        Integer obsoleteCount = 10;
        IdMappingResult idmappingResult =
                IdMappingResult.builder()
                        .unmappedIds(failedIds)
                        .suggestedIds(suggestIds)
                        .obsoleteCount(obsoleteCount)
                        .build();
        ExtraOptions result = IdMappingServiceUtils.getExtraOptions(idmappingResult);
        assertNotNull(result);
        assertEquals(result.getFailedIds(), failedIds);
        assertEquals(result.getSuggestedIds(), suggestIds);
        assertEquals(obsoleteCount, result.getObsoleteCount());
    }

    private IdMappingStringPair getMappingPair(String id) {
        return IdMappingStringPair.builder().from("from-" + id).to("to-" + id).build();
    }
}
