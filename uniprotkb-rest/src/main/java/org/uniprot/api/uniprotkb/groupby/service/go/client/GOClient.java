package org.uniprot.api.uniprotkb.groupby.service.go.client;

import java.util.List;

public interface GOClient {
    List<GoRelation> getChildren(String goId);
}
