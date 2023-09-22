package org.uniprot.api.support.data.statistics;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class StatisticsAttributeConfigTest {
    @Autowired private StatisticsAttributeConfig statisticsAttributeConfig;

    @Test
    void getFacetPropertyMap() {
        assertEquals(4, statisticsAttributeConfig.getFacetPropertyMap().size());
    }

    @Test
    void getFacetNames() {
        Collection<String> map = statisticsAttributeConfig.getFacetNames();
        assertThat(
                map, hasItems("comments", "sequence_amino_acid", "protein_existence", "features"));
    }
}
