package org.uniprot.api.help.centre.output;

import java.time.LocalDate;

import org.uniprot.core.json.parser.JsonConfig;
import org.uniprot.core.json.parser.serializer.LocalDateSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
public class HelpCentreJsonConfig extends JsonConfig {

    private static HelpCentreJsonConfig instance;

    private final ObjectMapper simpleMapper;

    private HelpCentreJsonConfig() {
        SimpleModule mod = new SimpleModule();
        mod.addSerializer(LocalDate.class, new LocalDateSerializer());

        ObjectMapper objMapper = getDefaultSimpleObjectMapper();
        objMapper.registerModule(mod);
        this.simpleMapper = objMapper;
    }

    public static synchronized HelpCentreJsonConfig getInstance() {
        if (instance == null) {
            instance = new HelpCentreJsonConfig();
        }
        return instance;
    }

    @Override
    public ObjectMapper getSimpleObjectMapper() {
        return simpleMapper;
    }

    @Override
    public ObjectMapper getFullObjectMapper() {
        throw new UnsupportedOperationException();
    }
}
