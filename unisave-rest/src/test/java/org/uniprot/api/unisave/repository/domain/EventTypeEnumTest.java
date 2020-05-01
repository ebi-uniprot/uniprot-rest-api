package org.uniprot.api.unisave.repository.domain;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created 01/05/2020
 *
 * @author Edd
 */
class EventTypeEnumTest {
    @Test
    void canGetMerged() {
        assertThat(EventTypeEnum.fromEventType("merged"), is(EventTypeEnum.MERGED));
    }

    @Test
    void canGetReplacing() {
        assertThat(EventTypeEnum.fromEventType("replacing"), is(EventTypeEnum.REPLACING));
    }

    @Test
    void canGetDeleted() {
        assertThat(EventTypeEnum.fromEventType("deleted"), is(EventTypeEnum.DELETED));
    }

    @Test
    void unknownTypeCausesException() {
        assertThrows(IllegalArgumentException.class, () -> EventTypeEnum.fromEventType("WRONG"));
    }
}
