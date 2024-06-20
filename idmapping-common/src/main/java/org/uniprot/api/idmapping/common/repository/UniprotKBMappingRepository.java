package org.uniprot.api.idmapping.common.repository;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.core.uniprotkb.DeletedReason;
import org.uniprot.core.uniprotkb.InactiveReasonType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.EntryInactiveReasonBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Repository
public class UniprotKBMappingRepository {

    private final SolrClient solrClient;

    public UniprotKBMappingRepository(@Qualifier("uniProtKBSolrClient") SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public UniProtKBEntry getDeletedEntry(String accession) {
        UniProtKBEntry result = null;
        try {
            SolrDocument solrDocument =
                    solrClient.getById(SolrCollection.uniprot.toString(), accession);
            if (solrDocument != null) {
                UniProtDocument document =
                        new DocumentObjectBinder().getBean(UniProtDocument.class, solrDocument);
                if (document.active) {
                    throw new QueryRetrievalException(
                            "Error fetching deleted entry for accession:" + accession);
                } else {
                    EntryInactiveReasonBuilder inactiveReasonBuilder =
                            new EntryInactiveReasonBuilder().type(InactiveReasonType.DELETED);
                    if (Utils.notNullNotEmpty(document.inactiveReason)) {
                        String[] inactiveReason = document.inactiveReason.split(":");
                        if (inactiveReason.length == 2) {
                            if (inactiveReason[0].equals(InactiveReasonType.DELETED.toString())) {
                                inactiveReasonBuilder.deletedReason(
                                        DeletedReason.valueOf(inactiveReason[1].strip()));
                            } else {
                                return getDeletedEntry(inactiveReason[1].strip());
                            }
                        }
                    }
                    String id = "";
                    if (Utils.notNullNotEmpty(document.id)) {
                        id = document.id.get(0);
                    }
                    UniProtKBEntryBuilder builder =
                            new UniProtKBEntryBuilder(accession, id, inactiveReasonBuilder.build());
                    if (Utils.notNull(document.uniparcDeleted)) {
                        builder.extraAttributesAdd(
                                UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB, document.uniparcDeleted);
                    }
                    result = builder.build();
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new QueryRetrievalException(
                    "Server error querying for deleted entries in Solr. accession:" + accession, e);
        }
        return result;
    }
}
