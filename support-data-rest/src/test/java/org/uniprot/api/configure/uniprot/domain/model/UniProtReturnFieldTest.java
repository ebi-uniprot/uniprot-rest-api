package org.uniprot.api.configure.uniprot.domain.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

/**
 * Created 15/03/20
 *
 * @author Edd
 */
class UniProtReturnFieldTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideValidUniProtDataTypes")
    void getReturnFieldsForValidUniProtDataTypeParameter(UniProtDataType validType) {
        List<UniProtReturnField> fields = UniProtReturnField.getReturnFieldsForClients(validType);
        fields.forEach(this::validField);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideInvalidUniProtDataTypes")
    void invalidUniProtDataTypeParameterCausesException(UniProtDataType invalidType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> UniProtReturnField.getReturnFieldsForClients(invalidType));
    }

    private static Stream<Arguments> provideValidUniProtDataTypes() {
        return Stream.of(UniProtDataType.UNIPROTKB).map(Arguments::of);
    }

    private static Stream<Arguments> provideInvalidUniProtDataTypes() {
        return Arrays.stream(UniProtDataType.values())
                .filter(type -> type != UniProtDataType.UNIPROTKB)
                .map(Arguments::of);
    }

    private void validField(UniProtReturnField field) {
        assertThat(field.getId(), not(isEmptyOrNullString()));

        if (Utils.notNullNotEmpty(field.getFields())) {
            assertThat(field.getGroupName(), not(isEmptyOrNullString()));
        } else {
            assertThat(field.getGroupName(), is(nullValue()));
            assertThat(field.getName(), not(isEmptyOrNullString()));
        }
    }
}
