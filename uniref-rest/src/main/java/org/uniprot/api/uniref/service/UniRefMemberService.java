package org.uniprot.api.uniref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.service.query.config.UniRefSolrQueryConfig;
import org.uniprot.api.uniref.repository.store.UniRefEntryStoreRepository;
import org.uniprot.api.uniref.request.UniRefMemberRequest;
import org.uniprot.core.uniref.UniRefMember;

/**
 * @author lgonzales
 * @since 05/01/2021
 */
@Service
@Import(UniRefSolrQueryConfig.class)
public class UniRefMemberService {

    private final UniRefEntryStoreRepository entryStoreRepository;

    @Autowired
    public UniRefMemberService(UniRefEntryStoreRepository entryStoreRepository) {
        this.entryStoreRepository = entryStoreRepository;
    }

    public QueryResult<UniRefMember> retrieveMembers(UniRefMemberRequest memberRequest) {
        try {
            return entryStoreRepository.getEntryMembers(memberRequest);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get members for id: [" + memberRequest.getId() + "]";
            throw new ServiceException(message, e);
        }
    }
}
