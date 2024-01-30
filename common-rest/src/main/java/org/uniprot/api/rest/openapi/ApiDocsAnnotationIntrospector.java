package org.uniprot.api.rest.openapi;

import java.util.Arrays;

import org.uniprot.core.util.EnumDisplay;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.cfg.PackageVersion;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class ApiDocsAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private static final long serialVersionUID = 891330293783978035L;

    public ApiDocsAnnotationIntrospector() {}

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        if (EnumDisplay.class.isAssignableFrom(enumType)) {
            return Arrays.stream(enumValues)
                    .map(
                            en -> {
                                EnumDisplay jsonEnum = (EnumDisplay) en;
                                return jsonEnum.getDisplayName();
                            })
                    .toArray(String[]::new);
        } else {
            return super.findEnumValues(enumType, enumValues, names);
        }
    }
}
