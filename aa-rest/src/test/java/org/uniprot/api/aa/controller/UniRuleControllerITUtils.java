package org.uniprot.api.aa.controller;

import org.uniprot.core.unirule.ConditionSet;
import org.uniprot.core.unirule.Rule;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.impl.ConditionBuilder;
import org.uniprot.core.unirule.impl.ConditionSetBuilder;
import org.uniprot.core.unirule.impl.ConditionValueBuilder;
import org.uniprot.core.unirule.impl.RuleBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilder;
import org.uniprot.core.unirule.impl.UniRuleIdBuilder;

/**
 * @author sahmad
 * @created 20/11/2020
 */
public class UniRuleControllerITUtils {
    public enum RuleType {UR, ARBA}
    static UniRuleEntry updateValidValues(UniRuleEntry uniRuleEntry, int suffix, RuleType ruleType) {
        UniRuleEntryBuilder builder = UniRuleEntryBuilder.from(uniRuleEntry);
        updateValidUniRuleId(builder, suffix, ruleType);
        updateMainRule(builder);
        return builder.build();
    }

    private static void updateValidUniRuleId(UniRuleEntryBuilder builder, int suffix, RuleType ruleType) {
        String uniRuleId = ruleType == RuleType.UR ? RuleType.UR + String.format("%09d", suffix)
                : RuleType.ARBA + String.format("%08d", suffix);
        UniRuleIdBuilder uniRuleIdBuilder = new UniRuleIdBuilder(uniRuleId);
        builder.uniRuleId(uniRuleIdBuilder.build());
    }

    private static void updateMainRule(UniRuleEntryBuilder builder) {
        Rule rule = builder.build().getMainRule();
        ConditionBuilder conditionBuilder = new ConditionBuilder("taxon");
        conditionBuilder.conditionValuesAdd(
                new ConditionValueBuilder("Archaea").cvId("2157").build());
        conditionBuilder.conditionValuesAdd(
                new ConditionValueBuilder("Eukaryota").cvId("2759").build());
        conditionBuilder.conditionValuesAdd(
                new ConditionValueBuilder("Bacteria").cvId("2").build());
        ConditionSet conditionSet = new ConditionSetBuilder(conditionBuilder.build()).build();

        ConditionBuilder conditionBuilder1 = new ConditionBuilder("scientific organism");
        conditionBuilder1.conditionValuesAdd(
                new ConditionValueBuilder("Archaea").cvId("2157").build());
        ConditionSet conditionSet1 = new ConditionSetBuilder(conditionBuilder1.build()).build();
        RuleBuilder ruleBuilder = RuleBuilder.from(rule);
        ruleBuilder.conditionSetsAdd(conditionSet);
        ruleBuilder.conditionSetsAdd(conditionSet1);
        builder.mainRule(ruleBuilder.build());
    }
}
