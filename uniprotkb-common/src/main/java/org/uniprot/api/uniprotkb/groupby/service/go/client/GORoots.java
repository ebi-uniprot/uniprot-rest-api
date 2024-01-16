package org.uniprot.api.uniprotkb.groupby.service.go.client;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("groupby.go")
@Component
@Data
public class GORoots {
    private List<GORoot> roots = new ArrayList<>();

    @Data
    @NoArgsConstructor
    static class GORoot {
        private String id;
        private String name;
    }
}
