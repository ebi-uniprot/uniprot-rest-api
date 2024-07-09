package org.uniprot.api.support.data.statistics.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.uniprot.api.support.data.statistics.entity.StatisticsCategory;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import({
    HttpCommonHeaderConfig.class,
    RequestMappingHandlerMapping.class,
    RequestMappingHandlerAdapter.class
})
class StatisticsCategoryRepositoryTest {
    @Autowired private TestEntityManager entityManager;

    @Autowired private StatisticsCategoryRepository statisticsCategoryRepository;

    @BeforeEach
    void setUp() {
        Arrays.stream(STATISTICS_CATEGORIES).forEach(entityManager::persist);
    }

    @Test
    void findByCategory() {
        Optional<StatisticsCategory> result =
                statisticsCategoryRepository.findByCategoryIgnoreCase("cat0");

        assertTrue(result.isPresent());
        assertThat(result.get(), equalTo(STATISTICS_CATEGORIES[0]));
    }

    @Test
    void findByCategory_whenCaseIsDiff() {
        Optional<StatisticsCategory> result =
                statisticsCategoryRepository.findByCategoryIgnoreCase("CaT0");

        assertTrue(result.isPresent());
        assertThat(result.get(), equalTo(STATISTICS_CATEGORIES[0]));
    }

    @Test
    void findByCategoryWhenNoMatch() {
        Optional<StatisticsCategory> result =
                statisticsCategoryRepository.findByCategoryIgnoreCase("cat3");

        assertFalse(result.isPresent());
    }
}
