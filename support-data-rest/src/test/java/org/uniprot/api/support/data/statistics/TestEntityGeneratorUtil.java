package org.uniprot.api.support.data.statistics;

import static org.uniprot.api.support.data.statistics.entity.EntryType.SWISSPROT;
import static org.uniprot.api.support.data.statistics.entity.EntryType.TREMBL;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import org.uniprot.api.support.data.statistics.entity.*;

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
    public static final Long[] ENTRY_IDS = new Long[] {34L, 411L, 999L, 1L, 29L, 392L};
    public static final String[] ENTRY_NAMES =
            new String[] {"name0", "name1", "name0", "name0", "name1", "name2"};
    public static final int[] STAT_CATEGORY_IDS = new int[] {0, 0, 1, 1, 2, 2};
    public static final Long[] VALUE_COUNTS = new Long[] {3L, 27L, 9999L, 500L, 87L, 55L};
    public static final Long[] ENTRY_COUNTS = new Long[] {31L, 25L, 188999L, 1098L, 510L, 67L};
    public static final String LABEL_0 = "label0";
    public static final String LABEL_1 = "label1";
    public static final String LABEL_2 = "label2";
    public static final String[] LABELS = new String[] {LABEL_0, LABEL_1, LABEL_2};
    public static final Long[] ATTRIBUTE_QUERY_IDS = new Long[] {123L, 3333L};
    public static final String[] QUERY_TEMPLATES =
            new String[] {"(query:query)", "(previous_release_date:previous_release_date)"};
    public static final String[] QUERIES =
            new String[] {
                "(reviewed:true) AND (query:query)",
                "(reviewed:true) AND (previous_release_date:2022-05-25)",
                "(reviewed:false) AND (query:query)",
                "(reviewed:true) AND (query:query)",
                "(reviewed:true) AND (previous_release_date:2022-05-25)"
            };
    public static final String[] QUERIES_COMMON =
            new String[] {
                "(query:query)",
                "(previous_release_date:2022-05-25)",
                "(query:query)",
                "(query:query)",
                "(previous_release_date:2022-05-25)"
            };
    public static final AttributeQuery[] ATTRIBUTE_QUERIES =
            new AttributeQuery[] {createAttributeQuery(0), createAttributeQuery(1)};
    public static final String[] SEARCH_FIELDS = new String[] {"sf0", "sf1", "sf2"};
    public static final String[] DESCRIPTIONS =
            new String[] {"des0", "des1", "des2", "des3", "des4", "des5"};
    public static final String REL_0 = "rel0";
    public static final String REL_1 = "rel1";
    public static final String REL_2 = "rel2";
    public static final Date[] DATES =
            new Date[] {
                Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now())
            };
    public static final UniProtRelease[] RELEASES =
            new UniProtRelease[] {
                createRelease(0, REL_0, DATES[0]),
                createRelease(1, REL_1, DATES[1]),
                createRelease(2, REL_2, DATES[2])
            };

    private static UniProtRelease createRelease(int id, String name, Date date) {
        UniProtRelease release = new UniProtRelease();
        release.setId(id);
        release.setName(name);
        release.setDate(date);
        return release;
    }

    private static AttributeQuery createAttributeQuery(int i) {
        AttributeQuery attributeQuery = new AttributeQuery();
        attributeQuery.setId(ATTRIBUTE_QUERY_IDS[i]);
        attributeQuery.setAttributeName(ENTRY_NAMES[i]);
        attributeQuery.setQuery(QUERY_TEMPLATES[i]);
        return attributeQuery;
    }

    public static final EntryType[] ENTRY_TYPES =
            new EntryType[] {SWISSPROT, SWISSPROT, TREMBL, SWISSPROT, SWISSPROT, TREMBL};
    public static final StatisticsCategory[] STATISTICS_CATEGORIES =
            new StatisticsCategory[] {
                createStatisticsCategory(0),
                createStatisticsCategory(1),
                createStatisticsCategory(2)
            };
    public static final UniProtKBStatisticsEntry[] STATISTICS_ENTRIES =
            new UniProtKBStatisticsEntry[] {
                createStatisticsEntry(0),
                createStatisticsEntry(1),
                createStatisticsEntry(2),
                createStatisticsEntry(3),
                createStatisticsEntry(4),
                createStatisticsEntry(5)
            };

    private static UniProtKBStatisticsEntry createStatisticsEntry(int index) {
        UniProtKBStatisticsEntry uniprotkbStatisticsEntry = new UniProtKBStatisticsEntry();
        uniprotkbStatisticsEntry.setId(ENTRY_IDS[index]);
        uniprotkbStatisticsEntry.setAttributeName(ENTRY_NAMES[index]);
        uniprotkbStatisticsEntry.setStatisticsCategory(
                STATISTICS_CATEGORIES[STAT_CATEGORY_IDS[index]]);
        uniprotkbStatisticsEntry.setValueCount(VALUE_COUNTS[index]);
        uniprotkbStatisticsEntry.setEntryCount(ENTRY_COUNTS[index]);
        uniprotkbStatisticsEntry.setDescription(DESCRIPTIONS[index]);
        uniprotkbStatisticsEntry.setUniprotRelease(
                Set.of(0, 1, 3, 4, 5).contains(index) ? RELEASES[0] : RELEASES[1]);
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
