package org.uniprot.api.uniref.common.service.light;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;

public class UniRefEntryLightUtils {

    static final int ID_LIMIT = 10;

    public static UniRefEntryLight cleanMemberId(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);

        List<String> members = removeMemberTypeFromMemberId(entry.getMembers());
        builder.membersSet(members);

        return builder.build();
    }

    public static UniRefEntryLight removeOverLimitAndCleanMemberId(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);

        List<String> members = entry.getMembers();
        if (entry.getMembers().size() > ID_LIMIT) {
            members = entry.getMembers().subList(0, ID_LIMIT);
        }

        members = removeMemberTypeFromMemberId(members);
        builder.membersSet(members);

        if (entry.getOrganisms().size() > ID_LIMIT) {
            LinkedHashSet<Organism> organisms =
                    entry.getOrganisms().stream()
                            .limit(ID_LIMIT)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            builder.organismsSet(organisms);
        }
        return builder.build();
    }

    /**
     * This method remove MemberIdType from member list and return just memberId
     *
     * @param members List of members that are stored in Voldemort with format:
     *     "memberId,MemberIdType"
     * @return List of return clean member with the format "memberId"
     */
    public static List<String> removeMemberTypeFromMemberId(List<String> members) {
        return members.stream()
                .map(memberId -> memberId.split(",")[0])
                .collect(Collectors.toList());
    }
}
