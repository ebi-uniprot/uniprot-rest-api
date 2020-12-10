package org.uniprot.api.uniref.repository.store;

import static org.uniprot.api.uniref.repository.store.UniRefEntryFacetConfig.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.store.BatchStoreIterable;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.api.uniref.service.UniRefEntryResult;
import org.uniprot.core.uniref.*;
import org.uniprot.core.uniref.impl.AbstractUniRefMemberBuilder;
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
    private final RetryPolicy<Object> uniRefMemberRetryPolicy;
    private final RetryPolicy<Object> uniRefLightRetryPolicy;
    private final UniRefMemberStoreClient unirefMemberStore;
    private final UniRefLightStoreClient uniRefLightStore;

    @Value("${search.default.page.size:#{null}}")
    protected Integer defaultPageSize;

    public UniRefEntryStoreRepository(
            UniRefMemberStoreClient unirefMemberStore,
            UniRefMemberStoreConfigProperties uniRefMemberConfig,
            UniRefLightStoreClient uniRefLightStore,
            UniRefLightStoreConfigProperties uniRefLightConfig) {
        this.unirefMemberStore = unirefMemberStore;
        this.uniRefLightStore = uniRefLightStore;
        this.uniRefLightRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(uniRefLightConfig.getFetchRetryDelayMillis()))
                        .withMaxRetries(uniRefLightConfig.getFetchMaxRetries());
        this.uniRefMemberRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofMillis(uniRefMemberConfig.getFetchRetryDelayMillis()))
                        .withMaxRetries(uniRefMemberConfig.getFetchMaxRetries());
    }

    public UniRefEntryResult getEntryById(String idValue, UniRefIdRequest uniRefIdRequest) {
        UniRefEntryResult.UniRefEntryResultBuilder builder = UniRefEntryResult.builder();
        UniRefEntryLight entryLight = getUniRefEntryLightFromStore(idValue);

        List<String> pageMemberIds =
                applyFacetFilters(entryLight.getMembers(), uniRefIdRequest.getFilter());
        boolean convertCommon = true;
        if (!uniRefIdRequest.isComplete()) {
            CursorPage page = getPage(uniRefIdRequest, pageMemberIds.size());
            builder.page(page);

            pageMemberIds = getMembersPage(page, pageMemberIds);
            convertCommon = (page.getOffset() == 0);
        }
        builder.entry(buildUniRefEntry(entryLight, pageMemberIds, convertCommon));
        return builder.build();
    }

    public List<Facet> getFacets(String idValue) {
        UniRefEntryLight entryLight = getUniRefEntryLightFromStore(idValue);
        return UniRefEntryFacetConfig.getFacets(entryLight);
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
            if (entryLight.getCommonTaxon() != null) {
                builder.commonTaxonId(entryLight.getCommonTaxon().getTaxonId());
                builder.commonTaxon(entryLight.getCommonTaxon().getScientificName());
            }
            builder.goTermsSet(entryLight.getGoTerms());
            builder.seedId(getSeedId(entryLight));
        }
        // build members
        BatchStoreIterable<RepresentativeMember> batchIterable =
                new BatchStoreIterable<>(
                        pageMemberIds,
                        unirefMemberStore,
                        uniRefMemberRetryPolicy,
                        unirefMemberStore.getMemberBatchSize());
        batchIterable.forEach(storedMembers -> convertMembers(storedMembers, builder, entryLight));
        return builder.build();
    }

    private void convertMembers(
            Collection<RepresentativeMember> storedMembers,
            UniRefEntryBuilder builder,
            UniRefEntryLight entryLight) {
        storedMembers.forEach(storedMember -> convertMember(storedMember, builder, entryLight));
    }

    private void convertMember(
            RepresentativeMember storedMember,
            UniRefEntryBuilder builder,
            UniRefEntryLight entryLight) {
        if (storedMember.getMemberId().equalsIgnoreCase(getRepresentativeMemberId(entryLight))) {
            RepresentativeMemberBuilder repBuilder = RepresentativeMemberBuilder.from(storedMember);
            cleanMemberFields(repBuilder, entryLight, storedMember.getMemberId());
            builder.representativeMember(repBuilder.build());
        } else {
            UniRefMemberBuilder memberBuilder = UniRefMemberBuilder.from(storedMember);
            cleanMemberFields(memberBuilder, entryLight, storedMember.getMemberId());
            builder.membersAdd(memberBuilder.build());
        }
    }

    private void cleanMemberFields(
            AbstractUniRefMemberBuilder<?, ?> builder,
            UniRefEntryLight entryLight,
            String memberId) {
        switch (entryLight.getEntryType()) {
            case UniRef100:
                builder.uniref100Id(null);
                break;
            case UniRef90:
                builder.uniref90Id(null);
                break;
            case UniRef50:
                builder.uniref50Id(null);
                break;
            default:
                break;
        }
        if (memberId.equalsIgnoreCase(getSeedMemberId(entryLight))) {
            builder.isSeed(true);
        }
    }

    private UniRefEntryLight getUniRefEntryLightFromStore(String clusterId) {
        return Failsafe.with(uniRefLightRetryPolicy)
                .get(() -> uniRefLightStore.getEntry(clusterId))
                .orElseThrow(() -> new ResourceNotFoundException(CLUSTER_ID_NOT_FOUND + clusterId));
    }

    private CursorPage getPage(UniRefIdRequest uniRefIdRequest, int memberCount) {
        if (uniRefIdRequest.getSize() == null) { // set the default result size
            uniRefIdRequest.setSize(defaultPageSize);
        }
        return CursorPage.of(uniRefIdRequest.getCursor(), uniRefIdRequest.getSize(), memberCount);
    }

    private List<String> getMembersPage(CursorPage page, List<String> members) {
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        return members.subList(offset, nextOffset);
    }

    private String getSeedId(UniRefEntryLight entryLight) {
        String[] splittedSeed = entryLight.getSeedId().split(",");
        return splittedSeed[splittedSeed.length - 1];
    }

    private String getSeedMemberId(UniRefEntryLight entryLight) {
        return entryLight.getSeedId().split(",")[0];
    }

    private String getRepresentativeMemberId(UniRefEntryLight entryLight) {
        return entryLight.getRepresentativeId().split(",")[0];
    }
}
