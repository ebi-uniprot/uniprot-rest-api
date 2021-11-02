package org.uniprot.api.aa.controller;

import static org.uniprot.core.ObjectsForTests.createEvidences;

import java.util.List;

import org.uniprot.core.ObjectsForTests;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.comment.SubcellularLocation;
import org.uniprot.core.uniprotkb.comment.SubcellularLocationValue;
import org.uniprot.core.uniprotkb.comment.impl.CatalyticActivityCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.CofactorCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.SubcellularLocationBuilder;
import org.uniprot.core.uniprotkb.comment.impl.SubcellularLocationCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.SubcellularLocationValueBuilder;
import org.uniprot.core.uniprotkb.evidence.Evidence;
import org.uniprot.core.uniprotkb.evidence.impl.EvidencedValueBuilder;
import org.uniprot.core.unirule.Annotation;
import org.uniprot.core.unirule.ConditionSet;
import org.uniprot.core.unirule.Rule;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.impl.AnnotationBuilder;
import org.uniprot.core.unirule.impl.AnnotationBuilderTest;
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
    public enum RuleType {
        UR,
        ARBA
    }

    static UniRuleEntry updateValidValues(
            UniRuleEntry uniRuleEntry, int suffix, RuleType ruleType) {
        UniRuleEntryBuilder builder = UniRuleEntryBuilder.from(uniRuleEntry);
        updateValidUniRuleId(builder, suffix, ruleType);
        updateMainRule(builder);
        return builder.build();
    }

    static UniRuleEntry updateAllCommentTypes(UniRuleEntry entry) {
        UniRuleEntryBuilder builder = UniRuleEntryBuilder.from(entry);
        RuleBuilder mainRuleBuilder = RuleBuilder.from(entry.getMainRule());
        // catalytic activity
        mainRuleBuilder.annotationsAdd(
                createAnnotationWithCommentType(CommentType.CATALYTIC_ACTIVITY));
        // similarity
        mainRuleBuilder.annotationsAdd(createAnnotationWithCommentType(CommentType.SIMILARITY));
        // cofactor with  note
        mainRuleBuilder.annotationsAdd(createAnnotationWithCommentType(CommentType.COFACTOR));
        // activity regulation
        mainRuleBuilder.annotationsAdd(
                createAnnotationWithCommentType(CommentType.ACTIVITY_REGULATION));
        // subcellular location with note
        mainRuleBuilder.annotationsAdd(
                createAnnotationWithCommentType(CommentType.SUBCELLULAR_LOCATION));
        // induction
        mainRuleBuilder.annotationsAdd(createAnnotationWithCommentType(CommentType.INDUCTION));
        // pathway
        mainRuleBuilder.annotationsAdd(createAnnotationWithCommentType(CommentType.PATHWAY));

        builder.mainRule(mainRuleBuilder.build());
        return builder.build();
    }

    private static void updateValidUniRuleId(
            UniRuleEntryBuilder builder, int suffix, RuleType ruleType) {
        String uniRuleId =
                ruleType == RuleType.UR
                        ? RuleType.UR + String.format("%09d", suffix)
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
        conditionBuilder.conditionValuesAdd(
                new ConditionValueBuilder("Yokapox virus").cvId("1076255").build());
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

    private static Annotation createAnnotationWithCommentType(CommentType commentType) {
        Annotation annotation = AnnotationBuilderTest.createObject(1, false);
        AnnotationBuilder annotationBuilder = AnnotationBuilder.from(annotation);
        switch (commentType) {
            case CATALYTIC_ACTIVITY:
                annotationBuilder.comment(
                        new CatalyticActivityCommentBuilder()
                                .reaction(ObjectsForTests.createReaction())
                                .build());
                break;
            case COFACTOR:
                annotationBuilder.comment(
                        new CofactorCommentBuilder()
                                .cofactorsSet(ObjectsForTests.cofactors())
                                .note(ObjectsForTests.createNote())
                                .build());
                break;
            case SUBCELLULAR_LOCATION:
                String locationVal = "some data";
                List<Evidence> evidences = createEvidences();
                String topologyVal = "some top";
                String orientationVal = "some orient";
                SubcellularLocationValue location =
                        new SubcellularLocationValueBuilder()
                                .id("id1")
                                .value(locationVal)
                                .evidencesSet(evidences)
                                .build();
                SubcellularLocationValue topology =
                        new SubcellularLocationValueBuilder()
                                .id("id2")
                                .value(topologyVal)
                                .evidencesSet(evidences)
                                .build();
                SubcellularLocationValue orientation =
                        new SubcellularLocationValueBuilder()
                                .id("id3")
                                .value(orientationVal)
                                .evidencesSet(evidences)
                                .build();
                SubcellularLocation sublocation =
                        new SubcellularLocationBuilder()
                                .location(location)
                                .topology(topology)
                                .orientation(orientation)
                                .build();
                annotationBuilder.comment(
                        new SubcellularLocationCommentBuilder()
                                .subcellularLocationsAdd(sublocation)
                                .note(ObjectsForTests.createNote())
                                .build());
                break;
            case PATHWAY:
            case INDUCTION:
            case ACTIVITY_REGULATION:
            case SIMILARITY:
                EvidencedValueBuilder eBuilder =
                        EvidencedValueBuilder.from(
                                ObjectsForTests.createEvidenceValueWithSingleEvidence());
                eBuilder.value("Belongs to the geminiviridae Rep protein family.");
                annotationBuilder.comment(
                        new FreeTextCommentBuilder()
                                .textsAdd(eBuilder.build())
                                .commentType(commentType)
                                .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported comment type " + commentType);
        }
        return annotationBuilder.build();
    }
}
