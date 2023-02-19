package org.uniprot.api.statistics.repository;

import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;

import static org.uniprot.api.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.statistics.entity.EntryType.TREMBL;

public class EntityGeneratorUtil {
    private static final Long[] STAT_IDS = new Long[]{12L, 91L};
    private static final String[] STAT_CATEGORIES_NAMES = new String[]{"cat0", "cat1"};
    private static final String DB_TYPE = "dbType";
    private static final Long[] ENTRY_IDS = new Long[]{34L, 411L, 999L, 1L};
    private static final String[] ENTRY_NAMES = new String[]{"name0", "name1", "name2", "name3"};
    private static final int[] STAT_CATEGORY_IDS = new int[]{0, 0, 1, 1};
    private static final Long[] VALUE_COUNTS = new Long[]{3L, 27L, 9999L, 500L};
    private static final Long[] ENTRY_COUNTS = new Long[]{31L, 25L, 188999L, 1098L};
    private static final String[] DESCRIPTIONS = new String[]{"des0", "des1", "des2", "des3"};
    private static final String[] RELEASES = new String[]{"rel0", "rel0", "rel1", "rel0"};
    private static final EntryType[] ENTRY_TYPES =
            new EntryType[]{SWISSPROT, SWISSPROT, TREMBL, SWISSPROT};
    static final StatisticsCategory[] STATISTICS_CATEGORIES =
            new StatisticsCategory[]{createStatisticsCategory(0), createStatisticsCategory(1)};
    static final UniprotkbStatisticsEntry[] STATISTICS_ENTRIES =
            new UniprotkbStatisticsEntry[]{
                    createStatisticsEntry(0),
                    createStatisticsEntry(1),
                    createStatisticsEntry(2),
                    createStatisticsEntry(3)
            };


    private static UniprotkbStatisticsEntry createStatisticsEntry(int index) {
        UniprotkbStatisticsEntry uniprotkbStatisticsEntry = new UniprotkbStatisticsEntry();
        uniprotkbStatisticsEntry.setId(ENTRY_IDS[index]);
        uniprotkbStatisticsEntry.setAttributeName(ENTRY_NAMES[index]);
        uniprotkbStatisticsEntry.setStatisticsCategoryId(STATISTICS_CATEGORIES[STAT_CATEGORY_IDS[index]]);
        uniprotkbStatisticsEntry.setValueCount(VALUE_COUNTS[index]);
        uniprotkbStatisticsEntry.setEntryCount(ENTRY_COUNTS[index]);
        uniprotkbStatisticsEntry.setDescription(DESCRIPTIONS[index]);
        uniprotkbStatisticsEntry.setReleaseName(RELEASES[index]);
        uniprotkbStatisticsEntry.setEntryType(ENTRY_TYPES[index]);
        return uniprotkbStatisticsEntry;
    }

    private static StatisticsCategory createStatisticsCategory(int index) {
        StatisticsCategory statisticsCategory = new StatisticsCategory();
        statisticsCategory.setId(STAT_IDS[index]);
        statisticsCategory.setCategory(STAT_CATEGORIES_NAMES[index]);
        statisticsCategory.setDbType(DB_TYPE);
        return statisticsCategory;
    }
}
