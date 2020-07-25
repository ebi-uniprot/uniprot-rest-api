package org.uniprot.api.uniref.repository.store;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.api.uniref.service.UniRefEntryResult;
import org.uniprot.core.uniref.RepresentativeMember;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryId;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryBuilder;
import org.uniprot.core.uniref.impl.UniRefMemberBuilder;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
@Slf4j
public class UniRefEntryStoreRepository {

    private static final String CLUSTER_ID_NOT_FOUND =
            "Unable to get UniRefEntry from store. ClusterId: ";
    private static final String MEMBER_NOT_FOUND = "Unable to get Member from store. ClusterId: ";
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);
    private final UniRefMemberStoreClient entryStore;
    private final UniRefLightStoreClient uniRefLightStore;

    public UniRefEntryStoreRepository(
            UniRefMemberStoreClient entryStore, UniRefLightStoreClient uniRefLightStore) {
        this.entryStore = entryStore;
        this.uniRefLightStore = uniRefLightStore;
    }

    public UniRefEntryResult getEntryById(String idValue, UniRefIdRequest uniRefIdRequest) {
        UniRefEntryResult.UniRefEntryResultBuilder builder = UniRefEntryResult.builder();
        UniRefEntryLight entryLight = getUniRefEntryLightFromStore(idValue);
        List<String> pageMemberIds = entryLight.getMembers();
        boolean convertCommon = true;
        if (!uniRefIdRequest.isComplete()) {
            CursorPage page = getPage(uniRefIdRequest, entryLight.getMemberCount());
            builder.page(page);
            pageMemberIds = getMembersPage(page, entryLight);
            convertCommon = (page.getOffset() == 0);
        }
        builder.entry(buildUniRefEntry(entryLight, pageMemberIds, convertCommon));
        return builder.build();
    }

    private UniRefEntry buildUniRefEntry(
            UniRefEntryLight entryLight, List<String> pageMemberIds, boolean convertCommon) {
        UniRefEntryBuilder builder = new UniRefEntryBuilder();

        if (convertCommon) { // convert common only in the first page
            builder.id(entryLight.getId());
            builder.name(entryLight.getName());
            builder.entryType(entryLight.getEntryType());
            builder.updated(entryLight.getUpdated());
            builder.memberCount(entryLight.getMemberCount());
            builder.commonTaxonId(entryLight.getCommonTaxonId());
            builder.commonTaxon(entryLight.getCommonTaxon());
            builder.goTermsSet(entryLight.getGoTerms());
        }
        // build members
        pageMemberIds.forEach(memberId -> convertMember(memberId, builder, entryLight));
        return builder.build();
    }

    private void convertMember(
            String memberId, UniRefEntryBuilder builder, UniRefEntryLight entryLight) {
        RepresentativeMember storedMember = getMemberFromStore(memberId, entryLight.getId());
        if (storedMember.getMemberId().equalsIgnoreCase(entryLight.getRepresentativeId())) {
            if (storedMember.getMemberId().equalsIgnoreCase(entryLight.getSeedId())) {
                storedMember = RepresentativeMemberBuilder.from(storedMember).isSeed(true).build();
            }
            builder.representativeMember(storedMember);
        } else {
            UniRefMemberBuilder memberBuilder = UniRefMemberBuilder.from(storedMember);
            if (storedMember.getMemberId().equalsIgnoreCase(entryLight.getSeedId())) {
                memberBuilder.isSeed(true);
            }
            builder.membersAdd(memberBuilder.build());
        }
    }

    private UniRefEntryLight getUniRefEntryLightFromStore(String clusterId) {
        return Failsafe.with(retryPolicy)
                .get(() -> uniRefLightStore.getEntry(clusterId))
                .orElseThrow(() -> new ResourceNotFoundException(CLUSTER_ID_NOT_FOUND + clusterId));
    }

    private RepresentativeMember getMemberFromStore(String memberId, UniRefEntryId clusterId) {
        return Failsafe.with(retryPolicy)
                .get(() -> entryStore.getEntry(memberId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        MEMBER_NOT_FOUND
                                                + clusterId.getValue()
                                                + ", memberId:"
                                                + memberId));
    }

    private CursorPage getPage(UniRefIdRequest uniRefIdRequest, int memberCount) {
        if (uniRefIdRequest.getSize() == null) { // set the default result size
            uniRefIdRequest.setSize(SearchRequest.DEFAULT_RESULTS_SIZE);
        }
        return CursorPage.of(uniRefIdRequest.getCursor(), uniRefIdRequest.getSize(), memberCount);
    }

    private List<String> getMembersPage(CursorPage page, UniRefEntryLight entryLight) {
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        if (nextOffset > entryLight.getMemberCount()) {
            nextOffset = entryLight.getMemberCount();
        }
        return entryLight.getMembers().subList(offset, nextOffset);
    }
}
