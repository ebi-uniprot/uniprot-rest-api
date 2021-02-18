package org.uniprot.api.idmapping.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class IdMappingStringPairTest {
    @Test
    void canCreatePair() {
        IdMappingStringPair pair =
                IdMappingStringPair.builder().from("from").to("to").build();
        assertThat(pair.getFrom(), is("from"));
        assertThat(pair.getTo(), is("to"));
    }
}
