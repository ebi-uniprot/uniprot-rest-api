package org.uniprot.api.uniref.repository.store;

import static org.uniprot.api.uniref.repository.store.UniRefEntryFacetConfig.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.uniref.request.UniRefMemberRequest;
import org.uniprot.core.uniref.*;
import org.uniprot.core.uniref.impl.AbstractUniRefMemberBuilder;
import org.uniprot.core.uniref.impl.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryBuilder;
import org.uniprot.core.uniref.impl.UniRefMemberBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.datastore.voldemort.RetrievalException;

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

    public UniRefEntry getEntryById(String idValue) {
        UniRefEntryLight entryLight = getUniRefEntryLightFromStore(idValue);

        UniRefEntryBuilder builder = new UniRefEntryBuilder();
        builder.id(entryLight.getId());
        builder.name(entryLight.getName());
        builder.entryType(entryLight.getEntryType());
        builder.updated(entryLight.getUpdated());
        builder.memberCount(entryLight.getMemberCount());
        if (entryLight.getCommonTaxon() != null) {
            builder.commonTaxon(entryLight.getCommonTaxon());
        }
        builder.goTermsSet(entryLight.getGoTerms());
        builder.seedId(getSeedId(entryLight));

        // build a cleaned representative member
        RepresentativeMemberBuilder repMemberBuilder =
                RepresentativeMemberBuilder.from(entryLight.getRepresentativeMember());
        cleanMemberFields(
                repMemberBuilder, entryLight, entryLight.getRepresentativeMember().getMemberId());
        builder.representativeMember(repMemberBuilder.build());

        // build members
        List<String> memberIds =
                entryLight.getMembers().stream()
                        .map(memberId -> memberId.split(",")[0])
                        .filter(
                                memberId ->
                                        !isRepresentative(
                                                memberId, entryLight.getRepresentativeMember()))
                        .collect(Collectors.toList());
        Stream<UniRefMember> members = getUniRefMembers(entryLight, memberIds);
        members.forEach(builder::membersAdd);

        return builder.build();
    }

    public QueryResult<UniRefMember> getEntryMembers(UniRefMemberRequest memberRequest) {
        UniRefEntryLight entryLight = getUniRefEntryLightFromStore(memberRequest.getId());
        List<String> members = entryLight.getMembers();
        log.debug("entryLight size {}", members.size());
        // Handle Facets
        members = applyFacetFilters(members, memberRequest.getFacetFilter());
        List<Facet> facets = getFacets(members, memberRequest.getFacets());
        log.debug("after facet size {}", members.size());
        // Build cursor page
        CursorPage page = getPage(memberRequest, members.size());

        // Get page member Ids
        log.debug("before get membersIdPage size {}", members.size());
        List<String> memberIds = getMembersIdPage(page, members);

        // Build members
        Stream<UniRefMember> uniRefMembers = getUniRefMembers(entryLight, memberIds);

        return QueryResult.<UniRefMember>builder()
                .content(uniRefMembers)
                .page(page)
                .facets(facets)
                .build();
    }

    private List<String> getMemberIds(List<String> members) {
        return members.stream()
                .map(memberId -> memberId.split(",")[0])
                .collect(Collectors.toList());
    }

    private Stream<UniRefMember> getUniRefMembers(
            UniRefEntryLight entryLight, List<String> memberIds) {
        BatchStoreIterable<RepresentativeMember> batchIterable =
                new BatchStoreIterable<>(
                        memberIds,
                        unirefMemberStore,
                        uniRefMemberRetryPolicy,
                        unirefMemberStore.getMemberBatchSize());
        return StreamSupport.stream(batchIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .map(storedMember -> mapMember(entryLight, storedMember));
    }

    private UniRefMember mapMember(UniRefEntryLight entryLight, RepresentativeMember storedMember) {
        if (isRepresentative(getRepresentativeId(entryLight), storedMember)) {
            RepresentativeMemberBuilder repBuilder = RepresentativeMemberBuilder.from(storedMember);
            cleanMemberFields(repBuilder, entryLight, storedMember.getMemberId());
            return repBuilder.build();
        } else {
            UniRefMemberBuilder memberBuilder = UniRefMemberBuilder.from(storedMember);
            cleanMemberFields(memberBuilder, entryLight, storedMember.getMemberId());
            return memberBuilder.build();
        }
    }

    private boolean isRepresentative(String memberId, RepresentativeMember repMember) {
        if (repMember.getMemberIdType().equals(UniRefMemberIdType.UNIPARC)) {
            return memberId.equals(repMember.getMemberId());
        } else if (Utils.notNullNotEmpty(repMember.getUniProtAccessions())) {
            String accession = repMember.getUniProtAccessions().get(0).getValue();
            return memberId.equals(accession);
        } else {
            throw new RetrievalException(
                    "UniRefMemberIdType.UNIPROTKB without accessions, RepMemberId: "
                            + repMember.getMemberId());
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

    private CursorPage getPage(UniRefMemberRequest memberRequest, int memberCount) {
        if (memberRequest.getSize() == null) { // set the default result size
            memberRequest.setSize(defaultPageSize);
        }
        return CursorPage.of(memberRequest.getCursor(), memberRequest.getSize(), memberCount);
    }

    private List<String> getMembersIdPage(CursorPage page, List<String> members) {
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        return getMemberIds(members.subList(offset, nextOffset));
    }

    private String getSeedId(UniRefEntryLight entryLight) {
        String[] splittedSeed = entryLight.getSeedId().split(",");
        return splittedSeed[splittedSeed.length - 1];
    }

    private String getSeedMemberId(UniRefEntryLight entryLight) {
        return entryLight.getSeedId().split(",")[0];
    }

    private String getRepresentativeId(UniRefEntryLight entryLight) {
        RepresentativeMember repMember = entryLight.getRepresentativeMember();
        if (UniRefMemberIdType.UNIPARC.equals(repMember.getMemberIdType())) {
            return repMember.getMemberId();
        } else {
            return repMember.getUniProtAccessions().get(0).getValue();
        }
    }
}
