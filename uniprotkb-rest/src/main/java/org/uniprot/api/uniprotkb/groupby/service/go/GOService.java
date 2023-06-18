package org.uniprot.api.uniprotkb.groupby.service.go;

import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GOClient;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoRelation;

import java.util.List;

@Service
public class GOService {
    private final GOClient goClient;

    public GOService(GOClient goClient) {
        this.goClient = goClient;
    }

    public List<GoRelation> getChildren(String parentGo) {
        return goClient.getChildren(parentGo);
    }
}
