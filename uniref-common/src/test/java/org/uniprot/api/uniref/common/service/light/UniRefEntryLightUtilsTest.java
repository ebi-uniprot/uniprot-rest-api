package org.uniprot.api.uniref.common.service.light;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;

class UniRefEntryLightUtilsTest {

    @Test
    void canCleanMemberId() {
        UniRefEntryLight entry =
                new UniRefEntryLightBuilder()
                        .membersSet(List.of("A,0", "B", "C,1", "D", "E,2"))
                        .build();
        UniRefEntryLight result = UniRefEntryLightUtils.cleanMemberId(entry);
        assertEquals(List.of("A", "B", "C", "D", "E"), result.getMembers());
    }

    @Test
    void canCleanEmptyMemberId() {
        UniRefEntryLight entry = new UniRefEntryLightBuilder().build();
        UniRefEntryLight result = UniRefEntryLightUtils.cleanMemberId(entry);
        assertEquals(List.of(), result.getMembers());
    }

    @Test
    void canRemoveOverLimitAndCleanMemberId() {
        LinkedHashSet<Organism> organisms = createOrganism(15);
        UniRefEntryLight entry =
                new UniRefEntryLightBuilder()
                        .membersSet(
                                List.of(
                                        "A,0", "B", "C,1", "D", "E,2", "F,0", "G", "H,1", "I",
                                        "J,2", "K,0", "L", "M,1", "N", "O,2"))
                        .organismsSet(organisms)
                        .build();
        UniRefEntryLight result = UniRefEntryLightUtils.removeOverLimitAndCleanMemberId(entry);
        assertEquals(UniRefEntryLightUtils.ID_LIMIT, result.getMembers().size());
        assertEquals(
                List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"), result.getMembers());
        assertEquals(UniRefEntryLightUtils.ID_LIMIT, result.getOrganisms().size());
    }

    @Test
    void canRemoveMemberTypeFromMemberId() {
        List<String> members = List.of("A,0", "B", "C,1", "D", "E,2", "F,2");
        List<String> result = UniRefEntryLightUtils.removeMemberTypeFromMemberId(members);
        assertEquals(List.of("A", "B", "C", "D", "E", "F"), result);
    }

    private LinkedHashSet<Organism> createOrganism(int total) {
        LinkedHashSet<Organism> organisms = new LinkedHashSet<>();
        IntStream.range(0, total)
                .forEach(
                        i -> {
                            organisms.add(
                                    new OrganismBuilder()
                                            .taxonId(9600 + i)
                                            .scientificName("name " + i)
                                            .build());
                        });
        return organisms;
    }
}
