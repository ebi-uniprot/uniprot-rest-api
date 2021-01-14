package org.uniprot.api.support.data.disease.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.support.data.disease.controller.download.IT.BaseDiseaseDownloadIT.SEARCH_ACCESSION1;
import static org.uniprot.api.support.data.disease.controller.download.IT.BaseDiseaseDownloadIT.SEARCH_ACCESSION2;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;

public class DiseaseSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver {

    @Override
    protected SearchContentTypeParam searchSuccessContentTypesParam() {
        String fmtStr = "format-version: 1.2";
        String defaultNSStr = "default-namespace: uniprot:diseases";
        String termStr =
                "name: ZTTK syndrome20\n"
                        + "def: \"An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.\" []\n"
                        + "synonym: \"Zhu-Tokita-Takenouchi-Kim syndrome\" [UniProt]\n"
                        + "synonym: \"ZTTK multiple congenital anomalies-mental retardation syndrome\" [UniProt]\n"
                        + "xref: MedGen20:CN23869020\n"
                        + "xref: MeSH20:D00001520\n"
                        + "xref: MeSH20:D00860720\n"
                        + "xref: MIM20:61714020 \"phenotype20\"";

        return SearchContentTypeParam.builder()
                .query("id:" + SEARCH_ACCESSION1 + " OR id:" + SEARCH_ACCESSION2)
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(MediaType.APPLICATION_JSON)
                                .resultMatcher(
                                        jsonPath(
                                                "$.results.*.id",
                                                containsInAnyOrder(
                                                        SEARCH_ACCESSION1, SEARCH_ACCESSION2)))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                .resultMatcher(content().string(containsString(SEARCH_ACCESSION1)))
                                .resultMatcher(content().string(containsString(SEARCH_ACCESSION2)))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                .resultMatcher(
                                        content()
                                                .string(
                                                        containsString(
                                                                "Name\tDiseaseEntry ID\tMnemonic\tDescription")))
                                .resultMatcher(
                                        content()
                                                .string(
                                                        containsString(
                                                                "ZTTK syndrome20\t"
                                                                        + SEARCH_ACCESSION2
                                                                        + "\tZTTKS20\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")))
                                .resultMatcher(
                                        content()
                                                .string(
                                                        containsString(
                                                                "ZTTK syndrome10\t"
                                                                        + SEARCH_ACCESSION1
                                                                        + "\tZTTKS10\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                .resultMatcher(
                                        content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                .resultMatcher(
                                        content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                                .resultMatcher(content().string(containsString(fmtStr)))
                                .resultMatcher(content().string(containsString(defaultNSStr)))
                                .resultMatcher(content().string(containsString(termStr)))
                                .build())
                .build();
    }

    @Override
    protected SearchContentTypeParam searchBadRequestContentTypesParam() {
        return SearchContentTypeParam.builder()
                .query("random_field:invalid")
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(MediaType.APPLICATION_JSON)
                                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                .resultMatcher(
                                        jsonPath(
                                                "$.messages.*",
                                                contains(
                                                        "'random_field' is not a valid search field")))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                .resultMatcher(content().string(isEmptyString()))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                .resultMatcher(content().string(isEmptyString()))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                .resultMatcher(content().string(isEmptyString()))
                                .build())
                .contentTypeParam(
                        ContentTypeParam.builder()
                                .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                .resultMatcher(content().string(isEmptyString()))
                                .build())
                .build();
    }
}
