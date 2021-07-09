package org.uniprot.api.help.centre.output;

import org.uniprot.core.json.parser.JsonConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
public class HelpCentreJsonConfig extends JsonConfig {

    private static HelpCentreJsonConfig instance;

    private final ObjectMapper simpleMapper;

    private HelpCentreJsonConfig() {
        this.simpleMapper = getDefaultSimpleObjectMapper();
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
