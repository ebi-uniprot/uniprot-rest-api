package org.uniprot.api.statistics;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;
import org.uniprot.api.statistics.repository.UniprotkbStatisticsEntryRepository;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled
public class RepoTest {
    @Autowired private UniprotkbStatisticsEntryRepository uniprotkbStatisticsEntryRepository;

    @Test
    void name() {
        List<UniprotkbStatisticsEntry> all = uniprotkbStatisticsEntryRepository.findAll();
        System.out.println(all);
    }
}
