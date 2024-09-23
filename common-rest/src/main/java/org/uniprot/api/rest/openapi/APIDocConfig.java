package org.uniprot.api.rest.openapi;

import static org.uniprot.core.util.Utils.*;
import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.*;
import java.util.stream.Collectors;

import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.core.citation.*;
import org.uniprot.core.uniprotkb.comment.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@EnableConfigurationProperties({OpenAPIConfiguration.class})
public class APIDocConfig {

    private static final List<String> STRING_SERIALISER_LIST =
            List.of(
                    "#/components/schemas/UniProtKBId",
                    "#/components/schemas/ProteomeId",
                    "#/components/schemas/Author",
                    "#/components/schemas/Journal",
                    "#/components/schemas/LocalDate",
                    "#/components/schemas/Locator",
                    "#/components/schemas/PublicationDate",
                    "#/components/schemas/UniParcId",
                    "#/components/schemas/ECNumber",
                    "#/components/schemas/FeatureDescription",
                    "#/components/schemas/FeatureId",
                    "#/components/schemas/Flag",
                    "#/components/schemas/IsoformId",
                    "#/components/schemas/UniProtKBDatabase",
                    "#/components/schemas/UniProtKBAccession",
                    "#/components/schemas/UniRefEntryId",
                    "#/components/schemas/UniRuleId");

    private static final Comparator<Parameter> PARAMETER_COMPARATOR =
            Comparator.comparing(Parameter::getRequired)
                    .reversed()
                    .thenComparing(Parameter::getName, Comparator.comparing("size"::equals))
                    .thenComparing(Parameter::getName, Comparator.comparing("download"::equals));

    @Bean
    SpringDocConfigProperties springDocConfigProperties() {
        return new SpringDocConfigProperties();
    }

    @Bean
    ObjectMapperProvider objectMapperProvider(SpringDocConfigProperties springDocConfigProperties) {
        ObjectMapperProvider mapperProvider = new ObjectMapperProvider(springDocConfigProperties);
        ObjectMapper openAPIMapper = mapperProvider.jsonMapper();
        openAPIMapper.setAnnotationIntrospector(new APIDocsAnnotationIntrospector());
        return mapperProvider;
    }

    @Bean
    public OpenApiCustomiser defaultOpenApiCustomiser(OpenAPIConfiguration openAPIConfiguration) {
        return openAPI -> {
            if (notNullNotEmpty(openAPIConfiguration.getServer())) {
                Server server = new Server();
                server.setDescription("UniProt REST API Server");
                server.setUrl(openAPIConfiguration.getServer());
                openAPI.setServers(List.of(server));
            }
            customiseSchema(openAPI);
            sortRequestParametersForGetAndPostMethods(openAPI.getPaths().values());
        };
    }

    private static void sortRequestParametersForGetAndPostMethods(Collection<PathItem> pathItems) {
        for (PathItem item : pathItems) {
            if (hasParameterToSort(item.getGet())) {
                item.getGet().getParameters().sort(PARAMETER_COMPARATOR);
            }
            if (hasParameterToSort(item.getPost())) {
                item.getPost().getParameters().sort(PARAMETER_COMPARATOR);
            }
        }
    }

    private static boolean hasParameterToSort(Operation operation) {
        return operation != null && notNullNotEmpty(operation.getParameters());
    }

    private void customiseSchema(OpenAPI openAPI) {
        Map<String, Schema> schemaMap = openAPI.getComponents().getSchemas();
        Schema commentParent = schemaMap.get("Comment");
        if (commentParent != null) {
            configureCommentSchema(openAPI, commentParent);
        }
        Schema citationParent = schemaMap.get("Citation");
        if (citationParent != null) {
            configureCitationSchema(openAPI, citationParent);
        }
        customiseSearchResultEntity(openAPI, schemaMap);
        for (Schema item : schemaMap.values()) {
            if ("Evidence".equals(item.getName())) {
                configureEvidenceSchema(item);
            }
            configureStringTypesSerialisers(item);
        }
        cleanStringSerialiserSchemaObjects(schemaMap);
    }

    private static void customiseSearchResultEntity(
            OpenAPI openAPI, Map<String, Schema> schemaMap) {
        List<String> searchEntries =
                openAPI.getPaths().values().stream()
                        .map(PathItem::getGet)
                        .filter(Objects::nonNull)
                        .map(Operation::getResponses)
                        .map(ApiResponses::getDefault)
                        .map(ApiResponse::getDescription)
                        .filter(s -> !s.equals("default response"))
                        .collect(Collectors.toList());
        if (!searchEntries.isEmpty()) {
            Schema searchResult = schemaMap.get("SearchResult");
            if (notNull(searchResult)) {
                updateSchemaItemsReference(searchResult, searchEntries);
            }
            Schema idMappingResult = schemaMap.get("IdMappingSearchResult");
            if (notNull(idMappingResult)) {
                updateAllOffItemsReference(idMappingResult, searchEntries);
            }
            Schema streamResult = schemaMap.get("StreamResult");
            if (notNull(streamResult) && notNull(idMappingResult)) {
                updateAllOffItemsReference(streamResult, searchEntries);
            } else if (notNull(streamResult)) {
                updateSchemaItemsReference(streamResult, searchEntries);
            }
        }
    }

    private static void updateAllOffItemsReference(
            Schema idMappingResult, List<String> searchEntries) {
        ArraySchema searchResults = (ArraySchema) idMappingResult.getProperties().get("results");
        searchResults.setAllOf(new ArrayList<>());
        for (String searchEntry : searchEntries) {
            Schema allOfSchema = new Schema();
            allOfSchema.set$ref("#/components/schemas/" + searchEntry);
            searchResults.getAllOf().add(allOfSchema);
        }
    }

    private static void updateSchemaItemsReference(Schema result, List<String> searchEntries) {
        ArraySchema searchResults = (ArraySchema) result.getProperties().get("results");
        searchResults.getItems().set$ref("#/components/schemas/" + searchEntries.get(0));
    }

    private static void cleanStringSerialiserSchemaObjects(Map<String, Schema> schemaMap) {
        // removing objects that are not being referenced anymore.
        for (String serialiser : STRING_SERIALISER_LIST) {
            String schemaKey = serialiser.substring(serialiser.lastIndexOf("/") + 1);
            schemaMap.remove(schemaKey);
        }
    }

    private static void configureStringTypesSerialisers(Schema item) {
        for (Object prop : item.getProperties().values()) {
            Schema propSchema = (Schema) prop;
            if (prop instanceof ArraySchema) {
                propSchema = propSchema.getItems();
            }
            if (propSchema.get$ref() != null
                    && STRING_SERIALISER_LIST.contains(propSchema.get$ref())) {
                // changing type from object to string
                propSchema.set$ref(null);
                propSchema.type("string");
            }
        }
    }

    private static void configureEvidenceSchema(Schema item) {
        Map<String, Schema> evidenceProps = item.getProperties();
        Schema stringSchema = evidenceProps.get("value");
        evidenceProps.put("id", stringSchema);
        evidenceProps.put("source", stringSchema);
        evidenceProps.put("evidenceCode", stringSchema);
        evidenceProps.remove("value");
        evidenceProps.remove("evidenceCrossReference");
    }

    private void configureCitationSchema(OpenAPI openAPI, Schema citationParent) {
        citationParent.setAllOf(new ArrayList<Schema>());
        Map<String, Schema> citations = new HashMap<>();
        citations.putAll(getSchemaReferenceMapForClass(Book.class, citationParent));
        citations.putAll(getSchemaReferenceMapForClass(ElectronicArticle.class, citationParent));
        citations.putAll(getSchemaReferenceMapForClass(JournalArticle.class, citationParent));
        citations.putAll(getSchemaReferenceMapForClass(Literature.class, citationParent));
        citations.putAll(getSchemaReferenceMapForClass(Patent.class, citationParent));
        citations.putAll(getSchemaReferenceMapForClass(Submission.class, citationParent));
        citations.putAll(getSchemaReferenceMapForClass(Thesis.class, citationParent));
        citations.entrySet().forEach(entry -> addSchemaToOpenApi(entry, openAPI, "citationType"));
    }

    private void configureCommentSchema(OpenAPI openAPI, Schema commentParent) {
        commentParent.setAllOf(new ArrayList<Schema>());
        Map<String, Schema> comments = new HashMap<>();
        comments.putAll(
                getSchemaReferenceMapForClass(AlternativeProductsComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(BPCPComment.class, commentParent));
        comments.putAll(
                getSchemaReferenceMapForClass(CatalyticActivityComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(CofactorComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(DiseaseComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(FreeTextComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(InteractionComment.class, commentParent));
        comments.putAll(
                getSchemaReferenceMapForClass(MassSpectrometryComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(RnaEditingComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(SequenceCautionComment.class, commentParent));
        comments.putAll(
                getSchemaReferenceMapForClass(SubcellularLocationComment.class, commentParent));
        comments.putAll(getSchemaReferenceMapForClass(WebResourceComment.class, commentParent));
        comments.entrySet().forEach(entry -> addSchemaToOpenApi(entry, openAPI, "commentType"));
    }

    private static void addSchemaToOpenApi(
            Map.Entry<String, Schema> entry, OpenAPI openAPI, String cleanFieldName) {
        Schema value = entry.getValue();
        value.getProperties().remove(cleanFieldName);
        openAPI.schema(entry.getKey(), value);
    }

    private Map<String, Schema> getSchemaReferenceMapForClass(Class<?> className, Schema parent) {
        AnnotatedType aType = new AnnotatedType(className).parent(parent).resolveAsRef(false);

        ResolvedSchema resolvedSchema =
                ModelConverters.getInstance().resolveAsResolvedSchema(aType);
        parent.addAllOfItem(getReferenceSchema(resolvedSchema.schema));
        return resolvedSchema.referencedSchemas;
    }

    private Schema getReferenceSchema(Schema fromSchema) {
        Schema schema = new Schema();
        schema.set$ref("#/components/schemas/" + fromSchema.getName());
        return schema;
    }
}
