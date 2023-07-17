package org.uniprot.api.uniprotkb.groupby.service.go.client;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;

@Component
public class GOClientImpl implements GOClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String goApiPrefix;
    private final GORoots goRoots;

    public GOClientImpl(@Value("${groupby.go.quickgo.url}") String goApiPrefix, GORoots goRoots) {
        this.goApiPrefix = goApiPrefix;
        this.goRoots = goRoots;
    }

    @Override
    public List<GoRelation> getChildren(String goId) {
        if (Strings.isNullOrEmpty(goId)) {
            return getRootChildren();
        }

        String url = goApiPrefix + goId + "/children";
        GoTermResult result = restTemplate.getForObject(url, GoTermResult.class);
        if (result == null || result.getResults().isEmpty()) {
            return List.of();
        } else {
            return result.getResults().get(0).getChildren();
        }
    }

    @Override
    public Optional<GoRelation> getGoEntry(String goId) {
        String url = goApiPrefix + goId + "/children";
        GoTermResult result = restTemplate.getForObject(url, GoTermResult.class);
        if (result == null || result.getResults().isEmpty()) {
            return Optional.empty();
        } else {
            GoTerm goTerm = result.getResults().get(0);
            return Optional.of(getGoRelation(goTerm.getId(), goTerm.getName()));
        }
    }

    private List<GoRelation> getRootChildren() {
        return goRoots.getRoots().stream()
                .map(goRoot -> getGoRelation(goRoot.getId(), goRoot.getName()))
                .collect(Collectors.toList());
    }

    private GoRelation getGoRelation(String id, String name) {
        GoRelation goRelation = new GoRelation();
        goRelation.setId(id);
        goRelation.setName(name);
        goRelation.setRelation("is_a");
        goRelation.setHasChildren(true);
        return goRelation;
    }
}
