package org.uniprot.api.uniref.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniref.repository.store.UniRefLightStoreClient;
import org.uniprot.api.uniref.repository.store.UniRefMemberStoreClient;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.core.uniref.RepresentativeMember;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryBuilder;
import org.uniprot.core.uniref.impl.UniRefMemberBuilder;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
@Slf4j
public class UniRefEntryConverter {

    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);
    private final UniRefMemberStoreClient entryStore;
    private final UniRefLightStoreClient uniRefLightStore;

    public UniRefEntryConverter(
            UniRefMemberStoreClient entryStore, UniRefLightStoreClient uniRefLightStore) {
        this.entryStore = entryStore;
        this.uniRefLightStore = uniRefLightStore;
    }

    public UniRefEntryResult convertEntry(UniRefDocument doc, UniRefIdRequest uniRefIdRequest) {
        UniRefEntryResult.UniRefEntryResultBuilder builder = UniRefEntryResult.builder();
        UniRefEntryLight entryLight = getUniRefEntryLightFromStore(doc.getId());
        List<String> pageMemberIds = entryLight.getMembers();
        boolean convertCommon = true;
        if (!uniRefIdRequest.isComplete()) {
            CursorPage page =
                    CursorPage.of(
                            uniRefIdRequest.getCursor(),
                            uniRefIdRequest.getSize(),
                            entryLight.getMemberCount());
            convertCommon = (page.getOffset() == 0); // convert common only in the first page
            pageMemberIds = getMembersPage(page, entryLight);
            builder.page(page);
        }
        builder.entry(buildUniRefEntry(entryLight, pageMemberIds, convertCommon));
        return builder.build();
    }

    private UniRefEntry buildUniRefEntry(
            UniRefEntryLight entryLight, List<String> pageMemberIds, boolean convertCommon) {
        UniRefEntryBuilder builder = new UniRefEntryBuilder();

        if (convertCommon) {
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
        RepresentativeMember storedMember = getMemberFromStore(memberId);
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
                .orElseThrow(
                        () ->
                                new ServiceException(
                                        "Unable to get UniRefEntryLight from store. ClusterId:"
                                                + clusterId));
    }

    private RepresentativeMember getMemberFromStore(String memberId) {
        return Failsafe.with(retryPolicy)
                .get(() -> entryStore.getEntry(memberId))
                .orElseThrow(
                        () ->
                                new ServiceException(
                                        "Unable to get RepresentativeMember from store. MemberId:"
                                                + memberId));
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
