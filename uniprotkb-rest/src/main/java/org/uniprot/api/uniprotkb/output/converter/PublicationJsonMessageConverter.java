package org.uniprot.api.uniprotkb.output.converter;

import java.util.Arrays;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.citation.impl.AuthorImpl;
import org.uniprot.core.citation.impl.JournalImpl;
import org.uniprot.core.citation.impl.PublicationDateImpl;
import org.uniprot.core.json.parser.JsonConfig;
import org.uniprot.core.json.parser.serializer.AuthorSerializer;
import org.uniprot.core.json.parser.serializer.JournalSerializer;
import org.uniprot.core.json.parser.serializer.PublicationDateSerializer;
import org.uniprot.core.json.parser.uniprot.serializer.EvidenceSerializer;
import org.uniprot.core.json.parser.uniprot.serializer.UniProtKBAccessionSerializer;
import org.uniprot.core.uniprotkb.evidence.impl.EvidenceImpl;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionImpl;
import org.uniprot.store.search.field.ReturnField;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author lgonzales
 * @since 2019-12-13
 */
public class PublicationJsonMessageConverter extends JsonMessageConverter<PublicationEntry> {

    public PublicationJsonMessageConverter() {
        super(
                PublicationJsonConfig.getInstance().getSimpleObjectMapper(),
                PublicationEntry.class,
                null); // TODO: fix return field before merge to master
    }

    enum ResultFields implements ReturnField {
        reference("reference"),
        statistics("statistics"),
        literatureMappedReference("literatureMappedReference"),
        categories("categories"),
        publicationSource("publicationSource");

        private String javaFieldName;

        ResultFields(String javaFieldName) {
            this.javaFieldName = javaFieldName;
        }

        @Override
        public boolean hasReturnField(String fieldName) {
            return Arrays.stream(ResultFields.values())
                    .anyMatch(returnItem -> returnItem.name().equalsIgnoreCase(fieldName));
        }

        @Override
        public String getJavaFieldName() {
            return javaFieldName;
        }
    }

    private static class PublicationJsonConfig extends JsonConfig {

        private static PublicationJsonConfig INSTANCE;

        private final ObjectMapper simpleObjectMapper;

        private PublicationJsonConfig() {
            simpleObjectMapper = initSimpleObjectMapper();
        }

        public static synchronized PublicationJsonConfig getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new PublicationJsonConfig();
            }
            return INSTANCE;
        }

        private ObjectMapper initSimpleObjectMapper() {
            ObjectMapper prettyObjMapper = getDefaultSimpleObjectMapper();

            SimpleModule simpleMod = new SimpleModule();
            simpleMod.addSerializer(JournalImpl.class, new JournalSerializer());
            simpleMod.addSerializer(AuthorImpl.class, new AuthorSerializer());
            simpleMod.addSerializer(
                    UniProtKBAccessionImpl.class, new UniProtKBAccessionSerializer());
            simpleMod.addSerializer(PublicationDateImpl.class, new PublicationDateSerializer());
            simpleMod.addSerializer(EvidenceImpl.class, new EvidenceSerializer());
            // TODO: 08/01/2021 serialise properly the mappedreferences

            prettyObjMapper.registerModule(simpleMod);
            return prettyObjMapper;
        }

        public ObjectMapper getSimpleObjectMapper() {
            return this.simpleObjectMapper;
        }

        @Override
        public ObjectMapper getFullObjectMapper() {
            return null;
        }
    }
}
