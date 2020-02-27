package org.uniprot.api.uniprotkb.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.store.config.searchfield.common.JsonLoader;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * The purpose of this class is to check whether the REST API documentation is aligned with the
 * actual fields defined in our schemas.
 *
 * <p>There are two types of test:
 *
 * <ul>
 *   <li>check that all fields defined in uniprotkb_query_param_meta.json are indeed valid fields
 *       known by {@link SearchFieldConfig}
 *   <li>check that all fields defined in {@link SearchFieldConfig} are documented, except for a
 *       predefined list of fields that <b>should not</b> be documented.
 * </ul>
 *
 * <p>Created 27/01/2020
 *
 * @author Edd
 */
class VerifyFieldsInAPIDocumentationExistTest {

    private static final String JSON_FILE = "uniprotkb_query_param_meta.json";
    private static final List<Predicate<String>> DOCUMENTED_FIELD_EXCLUSIONS;
    private static Set<String> documentedSearchFields;
    private static SearchFieldConfig searchFieldConfig;

    static {
        // predicates to match fields not to be documented in JSON_FILE
        DOCUMENTED_FIELD_EXCLUSIONS = new ArrayList<>();
        DOCUMENTED_FIELD_EXCLUSIONS.add(f -> !f.startsWith("xref_count"));
    }

    @BeforeAll
    static void beforeAll() {
        ObjectMapper mapper = getJsonMapper();
        JavaType type =
                mapper.getTypeFactory().constructCollectionType(List.class, QueryParamMeta.class);
        List<QueryParamMeta> things = JsonLoader.loadItems(JSON_FILE, mapper, type);
        documentedSearchFields =
                things.stream().map(QueryParamMeta::getName).collect(Collectors.toSet());
        searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.uniprotkb);
    }

    @ParameterizedTest(name = "{0} is a valid field?")
    @MethodSource("provideDocumentedSearchFields")
    void documentedSearchFieldIsKnownToSearchEngine(String documentedSearchField) {
        assertThat(
                documentedSearchField + " not found",
                searchFieldConfig.doesSearchFieldItemExist(documentedSearchField),
                is(true));
    }

    @ParameterizedTest(name = "{0} documented?")
    @MethodSource("provideSearchFields")
    void searchFieldHasBeenDocumented(String searchField) {
        assertThat(documentedSearchFields.contains(searchField), is(true));
    }

    private static Stream<Arguments> provideSearchFields() {
        return searchFieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(
                        field -> {
                            for (Predicate<String> predicate : DOCUMENTED_FIELD_EXCLUSIONS) {
                                if (!predicate.test(field)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                .map(Arguments::of);
    }

    private static Stream<Arguments> provideDocumentedSearchFields() {
        return documentedSearchFields.stream().map(Arguments::of);
    }

    private static ObjectMapper getJsonMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule();
        mod.addAbstractTypeMapping(QueryParamMeta.class, QueryParamMetaImpl.class);
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

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
