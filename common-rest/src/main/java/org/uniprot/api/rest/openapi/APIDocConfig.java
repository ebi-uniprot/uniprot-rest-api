package org.uniprot.api.rest.openapi;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.*;

import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.providers.ObjectMapperProvider;
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

@Configuration
public class APIDocConfig {

    private static final List<String> STRING_SERILISER_LIST =
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

    private static final Comparator<Parameter> parameterComparator =
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
    public OpenApiCustomiser defaultOpenApiCustomizer(ObjectMapperProvider objectMapperProvider) {
        return openAPI -> {
            customizeSchema(openAPI);
            sortRequestParametersForGetAndPostMethods(openAPI.getPaths().values());
        };
    }

    private static void sortRequestParametersForGetAndPostMethods(Collection<PathItem> pathItems) {
        for (PathItem item : pathItems) {
            if (hasParameterToSort(item.getGet())) {
                item.getGet().getParameters().sort(parameterComparator);
            }
            if (hasParameterToSort(item.getPost())) {
                item.getPost().getParameters().sort(parameterComparator);
            }
        }
    }

    private static boolean hasParameterToSort(Operation operation) {
        return operation != null && notNullNotEmpty(operation.getParameters());
    }

    private void customizeSchema(OpenAPI openAPI) {
        Map<String, Schema> schemaMap = openAPI.getComponents().getSchemas();
        Schema commentParent = schemaMap.get("Comment");
        if (commentParent != null) {
            configureCommentSchema(openAPI, commentParent);
        }
        Schema citationParent = schemaMap.get("Citation");
        if (citationParent != null) {
            configureCitationSchema(openAPI, citationParent);
        }

        for (Schema item : schemaMap.values()) {
            if ("Evidence".equals(item.getName())) {
                configureEvidenceSchema(item);
            }
            for (Object prop : item.getProperties().values()) {
                Schema propSchema = (Schema) prop;
                if (prop instanceof ArraySchema) {
                    propSchema = propSchema.getItems();
                }
                if (propSchema.get$ref() != null
                        && STRING_SERILISER_LIST.contains(propSchema.get$ref())) {
                    propSchema.set$ref(null);
                    propSchema.type("string");
                }
            }
        }
        for (String serialiser : STRING_SERILISER_LIST) {
            String schemaKey = serialiser.substring(serialiser.lastIndexOf("/") + 1);
            schemaMap.remove(schemaKey);
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
        getSchemaReferenceMapForClass(Book.class, citationParent).forEach(openAPI::schema);
        getSchemaReferenceMapForClass(ElectronicArticle.class, citationParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(JournalArticle.class, citationParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(Literature.class, citationParent).forEach(openAPI::schema);
        getSchemaReferenceMapForClass(Patent.class, citationParent).forEach(openAPI::schema);
        getSchemaReferenceMapForClass(Submission.class, citationParent).forEach(openAPI::schema);
        getSchemaReferenceMapForClass(Thesis.class, citationParent).forEach(openAPI::schema);
    }

    private void configureCommentSchema(OpenAPI openAPI, Schema commentParent) {
        commentParent.setAllOf(new ArrayList<Schema>());
        getSchemaReferenceMapForClass(AlternativeProductsComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(BPCPComment.class, commentParent).forEach(openAPI::schema);
        getSchemaReferenceMapForClass(CatalyticActivityComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(CofactorComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(DiseaseComment.class, commentParent).forEach(openAPI::schema);
        getSchemaReferenceMapForClass(FreeTextComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(InteractionComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(MassSpectrometryComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(RnaEditingComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(SequenceCautionComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(SubcellularLocationComment.class, commentParent)
                .forEach(openAPI::schema);
        getSchemaReferenceMapForClass(WebResourceComment.class, commentParent)
                .forEach(openAPI::schema);
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
