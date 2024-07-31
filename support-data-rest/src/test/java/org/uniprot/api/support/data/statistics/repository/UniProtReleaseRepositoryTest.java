package org.uniprot.api.support.data.statistics.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
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
import org.uniprot.api.support.data.statistics.entity.UniProtRelease;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import({
    HttpCommonHeaderConfig.class,
    RequestMappingHandlerMapping.class,
    RequestMappingHandlerAdapter.class
})
class UniProtReleaseRepositoryTest {
    @Autowired private TestEntityManager entityManager;

    @Autowired private UniProtReleaseRepository uniProtReleaseRepository;
    private static final String[] IDS = new String[] {"2023_05", "2023_06", "2024_01", "2024_02"};
    private static final Date[] DATES =
            new Date[] {
                Date.from(Instant.ofEpochMilli(100L)),
                Date.from(Instant.ofEpochMilli(200L)),
                Date.from(Instant.ofEpochMilli(300L)),
                Date.from(Instant.ofEpochMilli(400L))
            };
    private static final UniProtRelease[] UNIPROT_RELEASES = new UniProtRelease[4];

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 4; i++) {
            UniProtRelease uniProtRelease = new UniProtRelease();
            uniProtRelease.setId(IDS[i]);
            uniProtRelease.setDate(DATES[i]);
            UNIPROT_RELEASES[i] = uniProtRelease;
        }
        Arrays.stream(UNIPROT_RELEASES).forEach(entityManager::persist);
    }

    @Test
    void findPreviousReleaseDate_forSameYear() {
        Optional<Date> previousReleaseDate =
                uniProtReleaseRepository.findPreviousReleaseDate("2024_02");

        assertEquals(DATES[2], previousReleaseDate.get());
    }

    @Test
    void findPreviousReleaseDate_forDifferentYear() {
        Optional<Date> previousReleaseDate =
                uniProtReleaseRepository.findPreviousReleaseDate("2024_01");

        assertEquals(DATES[1], previousReleaseDate.get());
    }
}
