package org.uniprot.api.support.data.statistics.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.uniprot.api.support.data.statistics.TestEntityGeneratorUtil.STATISTICS_CATEGORIES;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.support.data.statistics.entity.AttributeQuery;
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import({
    HttpCommonHeaderConfig.class,
    RequestMappingHandlerMapping.class,
    RequestMappingHandlerAdapter.class
})
class AttributeQueryRepositoryTest {
    @Autowired private TestEntityManager entityManager;

    @Autowired private AttributeQueryRepository attributeQueryRepository;
    private static final Long[] IDS = new Long[] {0L, 1L, 2L};
    private static final String[] ATTRIBUTES = new String[] {"a0", "a1", "a2"};
    private static final String[] QUERIES = new String[] {"q0", "q1", "q2"};
    private static final AttributeQuery[] ATTRIBUTE_QUERIES = new AttributeQuery[3];
    private static final StatisticsCategory STATISTICS_CATEGORY = STATISTICS_CATEGORIES[0];

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 3; i++) {
            AttributeQuery attributeQuery = new AttributeQuery();
            attributeQuery.setId(IDS[i]);
            attributeQuery.setAttributeName(ATTRIBUTES[i]);
            attributeQuery.setStatisticsCategory(STATISTICS_CATEGORY);
            attributeQuery.setQuery(QUERIES[i]);
            ATTRIBUTE_QUERIES[i] = attributeQuery;
        }
        Arrays.stream(ATTRIBUTE_QUERIES).forEach(entityManager::persist);
        entityManager.persist(STATISTICS_CATEGORY);
    }

    @Test
    void findByAttributeName_whenExist() {
        Optional<AttributeQuery> result =
                attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORY, ATTRIBUTES[1]);
        AttributeQuery attributeQuery = result.get();

        assertEquals(IDS[1], attributeQuery.getId());
        assertEquals(QUERIES[1], attributeQuery.getQuery());
    }

    @Test
    void findByAttributeName_whenExistAndCaseDiff() {
        Optional<AttributeQuery> result =
                attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORY, ATTRIBUTES[1].toUpperCase());
        AttributeQuery attributeQuery = result.get();

        assertEquals(IDS[1], attributeQuery.getId());
        assertEquals(QUERIES[1], attributeQuery.getQuery());
    }

    @Test
    void findByAttributeName_whenAbsent() {
        Optional<AttributeQuery> result =
                attributeQueryRepository.findByStatisticsCategoryAndAttributeNameIgnoreCase(STATISTICS_CATEGORY, "random");

        assertFalse(result.isPresent());
    }
}
