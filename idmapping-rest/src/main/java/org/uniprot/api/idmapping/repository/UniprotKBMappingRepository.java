package org.uniprot.api.idmapping.repository;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
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
                    String id = "";
                    if (Utils.notNullNotEmpty(document.id)) {
                        id = document.id.get(0);
                    }
                    result =
                            new UniProtKBEntryBuilder(accession, id, inactiveReasonBuilder.build())
                                    .build();
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new QueryRetrievalException(
                    "Server error querying for deleted entries in Solr. accession:" + accession, e);
        }
        return result;
    }
}
