package org.uniprot.api.uniprotkb.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.store.search.domain2.JsonLoader;
import org.uniprot.store.search.domain2.SearchField;
import org.uniprot.store.search.field.UniProtSearchFields;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created 27/01/2020
 *
 * @author Edd
 */
public class VerifyFieldsInAPIDocumentationExist {

    private static final String JSON_FILE = "uniprotkb_query_param_meta.json";
    private static Set<QueryParamMeta> documentedSearchFields;

    @BeforeAll
    static void beforeAll() {
        ObjectMapper mapper = getJsonMapper(QueryParamMeta.class, QueryParamMetaImpl.class);
        JavaType type =
                mapper.getTypeFactory().constructCollectionType(List.class, QueryParamMeta.class);
        Set<QueryParamMeta> things = JsonLoader.loadItems(JSON_FILE, mapper, type);

                        .stream().map(QueryParamMeta::getName).collect(Collectors.toSet());
    }

    @ParameterizedTest(name = "{0} is a valid field?")
    @MethodSource("provideDocumentedSearchFields")
    void documentedSearchFieldIsKnownToSearchEngine(String documentedSearchField) {
        assertThat(UniProtSearchFields.UNIPROTKB.hasField(documentedSearchField), is(true));
    }

    @ParameterizedTest(name = "{0} documented?")
    @MethodSource("provideSearchFields")
    void searchFieldHasBeenDocumented(String searchField) {
        assertThat(documentedSearchFields.contains(searchField), is(true));
    }

    private static Stream<Arguments> provideSearchFields() {
        Set<String> fieldsExcludedFromDocumentation = new HashSet<>();
        fieldsExcludedFromDocumentation.add("asdf");
        return UniProtSearchFields.UNIPROTKB.getSearchFields().stream()
                .map(SearchField::getName)
                .filter(f -> !fieldsExcludedFromDocumentation.contains(f))
                .map(Arguments::of);
    }

    private static Stream<Arguments> provideDocumentedSearchFields() {
        return documentedSearchFields.stream().map(QueryParamMeta::getName).map(Arguments::of);
    }

    private static <I, C extends I> ObjectMapper getJsonMapper(Class<I> clazz, Class<C> subClazz) {
        final ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule();
        mod.addAbstractTypeMapping(clazz, subClazz);
        objectMapper.registerModule(mod);
        return objectMapper;
    }

    interface QueryParamMeta {
        String getName();

        String getType();
    }

    static class QueryParamMetaImpl implements QueryParamMeta {
        private String name;
        private String type;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return type;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
