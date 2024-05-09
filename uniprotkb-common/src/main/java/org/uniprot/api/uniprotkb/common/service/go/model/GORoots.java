package org.uniprot.api.uniprotkb.common.service.go.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.NoArgsConstructor;

@ConfigurationProperties("groupby.go")
@Component
@Data
public class GORoots {
    private List<GORoot> roots = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class GORoot {
        private String id;
        private String name;
    }
}
