package org.uniprot.api.uniprotkb.view.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.view.GoRelation;

@Service
public class GoService {
    private final GoClient goClient;

    public GoService(GoClient goClient) {
        this.goClient = goClient;
    }

    public List<GoRelation> getChildren(String parentGo) {
        return goClient.getChildren(parentGo);
    }
}
