package org.uniprot.api.idmapping.common.service.impl;

import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.service.UniProtKBIdGroupByService;
import org.uniprot.api.uniprotkb.common.service.go.model.GoRelation;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByGOService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

@Service
public class UniProtKBIdGroupByGOService extends UniProtKBIdGroupByService<GoRelation> {
    public UniProtKBIdGroupByGOService(
            GroupByGOService groupByGOService, UniProtEntryService uniProtEntryService) {
        super(groupByGOService, uniProtEntryService);
    }
}
