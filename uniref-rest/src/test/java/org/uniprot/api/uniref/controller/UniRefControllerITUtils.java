package org.uniprot.api.uniref.controller;

import org.uniprot.core.Sequence;
import org.uniprot.core.cv.go.GoAspect;
import org.uniprot.core.cv.go.impl.GeneOntologyEntryBuilder;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniparc.impl.UniParcIdBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionBuilder;
import org.uniprot.core.uniref.*;
import org.uniprot.core.uniref.impl.*;

import java.time.LocalDate;

/**
 * @author jluo
 * @date: 23 Aug 2019
 */
class UniRefControllerITUtils {

    static final String ID_PREF_50 = "UniRef50_P039";
    static final String ID_PREF_90 = "UniRef90_P039";
    static final String ID_PREF_100 = "UniRef100_P039";
    static final String NAME_PREF = "Cluster: MoeK5 ";
    static final String ACC_PREF = "P123";
    static final String ACC_2_PREF = "P123";
    static final String UPI_PREF = "UPI0000083A";

    static UniRefEntryLight createEntryLight(int i, UniRefType type) {
        UniRefEntry entry = createEntry(i, type);
        UniRefEntryLightBuilder builder =
                new UniRefEntryLightBuilder()
                        .id(entry.getId())
                        .name(entry.getName())
                        .updated(entry.getUpdated())
                        .entryType(entry.getEntryType())
                        .commonTaxonId(entry.getCommonTaxonId())
                        .commonTaxon(entry.getCommonTaxon())
                        .memberIdTypesAdd(entry.getRepresentativeMember().getMemberIdType())
                        .membersAdd(entry.getRepresentativeMember().getMemberId())
                        .organismsAdd(entry.getRepresentativeMember().getOrganismName())
                        .organismIdsAdd(entry.getRepresentativeMember().getOrganismTaxId());
        entry.getMembers()
                .forEach(
                        uniRefMember -> {
                            builder.memberIdTypesAdd(uniRefMember.getMemberIdType())
                                    .membersAdd(uniRefMember.getMemberId())
                                    .organismsAdd(uniRefMember.getOrganismName())
                                    .organismIdsAdd(uniRefMember.getOrganismTaxId());
                        });
        return builder.memberCount(entry.getMemberCount()).build();
    }

    static UniRefEntry createEntry(int i, UniRefType type) {
        String idRef = getIdRef(type);

        UniRefEntryId entryId = new UniRefEntryIdBuilder(getName(idRef, i)).build();

        return new UniRefEntryBuilder()
                .id(entryId)
                .name(getName(NAME_PREF, i))
                .updated(LocalDate.of(2019, 8, 27))
                .entryType(type)
                .commonTaxonId(9606L)
                .commonTaxon("Homo sapiens")
                .representativeMember(createReprestativeMember(i))
                .membersAdd(createMember(i))
                .goTermsAdd(
                        new GeneOntologyEntryBuilder()
                                .aspect(GoAspect.COMPONENT)
                                .id("GO:0044444")
                                .build())
                .goTermsAdd(
                        new GeneOntologyEntryBuilder()
                                .aspect(GoAspect.FUNCTION)
                                .id("GO:0044459")
                                .build())
                .goTermsAdd(
                        new GeneOntologyEntryBuilder()
                                .aspect(GoAspect.PROCESS)
                                .id("GO:0032459")
                                .build())
                .memberCount(2)
                .build();
    }

    private static String getIdRef(UniRefType type) {
        switch (type) {
            case UniRef50:
                return ID_PREF_50;
            case UniRef90:
                return ID_PREF_90;
            default:
                return ID_PREF_100;
        }
    }

    static UniRefMember createMember(int i) {
        String memberId = getName(ACC_2_PREF, i) + "_HUMAN";
        int length = 312;
        String pName = "some protein name";
        String upi = getName(UPI_PREF, i);

        UniRefMemberIdType type = UniRefMemberIdType.UNIPROTKB;
        return new UniRefMemberBuilder()
                .memberIdType(type)
                .memberId(memberId)
                .organismName("Homo sapiens")
                .organismTaxId(9606)
                .sequenceLength(length)
                .proteinName(pName)
                .uniparcId(new UniParcIdBuilder(upi).build())
                .accessionsAdd(new UniProtKBAccessionBuilder(getName(ACC_2_PREF, i)).build())
                .uniref100Id(new UniRefEntryIdBuilder("UniRef100_P03923").build())
                .uniref90Id(new UniRefEntryIdBuilder("UniRef90_P03943").build())
                .uniref50Id(new UniRefEntryIdBuilder("UniRef50_P03973").build())
                .build();
    }

    static String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    static RepresentativeMember createReprestativeMember(int i) {
        String seq = "MVSWGRFICLVVVTMATLSLARPSFSLVEDDFSAGSADFAFWERDGDSDGFDSHSDJHETRHJREH";
        Sequence sequence = new SequenceBuilder(seq).build();
        String memberId = getName(ACC_PREF, i) + "_HUMAN";
        int length = 312;
        String pName = "some protein name";
        String upi = getName(UPI_PREF, i);

        UniRefMemberIdType type = UniRefMemberIdType.UNIPROTKB;

        return new RepresentativeMemberBuilder()
                .memberIdType(type)
                .memberId(memberId)
                .organismName("Homo sapiens")
                .organismTaxId(9606)
                .sequenceLength(length)
                .proteinName(pName)
                .uniparcId(new UniParcIdBuilder(upi).build())
                .accessionsAdd(new UniProtKBAccessionBuilder(getName(ACC_PREF, i)).build())
                .uniref100Id(new UniRefEntryIdBuilder("UniRef100_P03923").build())
                .uniref90Id(new UniRefEntryIdBuilder("UniRef90_P03943").build())
                .uniref50Id(new UniRefEntryIdBuilder("UniRef50_P03973").build())
                .isSeed(true)
                .sequence(sequence)
                .build();
    }
}
