package org.uniprot.api.support.data.configure.response.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
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

        // root
        fields.forEach(this::validField);

        // fields
        fields.stream().flatMap(field -> field.getFields().stream()).forEach(this::validField);

        boolean hasAtLeastOneFilter =
                fields.stream()
                        .flatMap(field -> field.getFields().stream())
                        .anyMatch(field -> Utils.notNullNotEmpty(field.getSortField()));
        Assertions.assertTrue(hasAtLeastOneFilter);
    }

    private static Stream<Arguments> provideValidUniProtDataTypes() {
        return Stream.of(UniProtDataType.UNIPROTKB).map(Arguments::of);
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
