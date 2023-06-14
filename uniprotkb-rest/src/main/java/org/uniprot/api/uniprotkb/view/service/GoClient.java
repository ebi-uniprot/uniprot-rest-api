package org.uniprot.api.uniprotkb.view.service;

import java.util.List;

import org.uniprot.api.uniprotkb.view.GoRelation;

public interface GoClient {
    List<GoRelation> getChildren(String goId);
}
