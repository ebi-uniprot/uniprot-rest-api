package org.uniprot.api.statistics;

import static org.uniprot.api.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.statistics.entity.EntryType.TREMBL;

import org.uniprot.api.statistics.entity.EntryType;
import org.uniprot.api.statistics.entity.StatisticsCategory;
import org.uniprot.api.statistics.entity.UniprotkbStatisticsEntry;

public class TestEntityGeneratorUtil {

    public static final String RELEASE = "rel";
    public static final String STATISTIC_TYPE = "statType";
    public static final Long[] STAT_IDS = new Long[] {12L, 91L, 55888L};
    public static final String CATEGORY_0 = "cat0";
    public static final String CATEGORY_1 = "cat1";
    public static final String CATEGORY_2 = "cat2";
    public static final String[] STAT_CATEGORIES_NAMES =
            new String[] {CATEGORY_0, CATEGORY_1, CATEGORY_2};
    public static final String DB_TYPE = "dbType";
    public static final Long[] ENTRY_IDS = new Long[] {34L, 411L, 999L, 1L, 29L};
    public static final String[] ENTRY_NAMES =
            new String[] {"name0", "name1", "name2", "name3", "name4"};
    public static final int[] STAT_CATEGORY_IDS = new int[] {0, 0, 1, 1, 2};
    public static final Long[] VALUE_COUNTS = new Long[] {3L, 27L, 9999L, 500L, 87L};
    public static final Long[] ENTRY_COUNTS = new Long[] {31L, 25L, 188999L, 1098L, 510L};
    public static final String LABEL_0 = "label0";
    public static final String LABEL_1 = "label1";
    public static final String LABEL_2 = "label2";
    public static final String[] LABELS = new String[] {LABEL_0, LABEL_1, LABEL_2};
    public static final String[] SEARCH_FIELDS = new String[] {"sf0", "sf1", "sf2"};
    public static final String[] DESCRIPTIONS =
            new String[] {"des0", "des1", "des2", "des3", "des4"};
    public static final String[] RELEASES = new String[] {"rel0", "rel0", "rel1", "rel0", "rel0"};
    public static final EntryType[] ENTRY_TYPES =
            new EntryType[] {SWISSPROT, SWISSPROT, TREMBL, SWISSPROT, SWISSPROT};
    public static final StatisticsCategory[] STATISTICS_CATEGORIES =
            new StatisticsCategory[] {
                createStatisticsCategory(0),
                createStatisticsCategory(1),
                createStatisticsCategory(2)
            };
    public static final UniprotkbStatisticsEntry[] STATISTICS_ENTRIES =
            new UniprotkbStatisticsEntry[] {
                createStatisticsEntry(0),
                createStatisticsEntry(1),
                createStatisticsEntry(2),
                createStatisticsEntry(3),
                createStatisticsEntry(4)
            };

    private static UniprotkbStatisticsEntry createStatisticsEntry(int index) {
        UniprotkbStatisticsEntry uniprotkbStatisticsEntry = new UniprotkbStatisticsEntry();
        uniprotkbStatisticsEntry.setId(ENTRY_IDS[index]);
        uniprotkbStatisticsEntry.setAttributeName(ENTRY_NAMES[index]);
        uniprotkbStatisticsEntry.setStatisticsCategory(
                STATISTICS_CATEGORIES[STAT_CATEGORY_IDS[index]]);
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
        statisticsCategory.setLabel(LABELS[index]);
        statisticsCategory.setSearchField(SEARCH_FIELDS[index]);
        return statisticsCategory;
    }
}
