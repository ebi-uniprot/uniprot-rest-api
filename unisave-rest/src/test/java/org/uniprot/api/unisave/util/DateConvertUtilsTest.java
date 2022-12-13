package org.uniprot.api.unisave.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class DateConvertUtilsTest {

    @Test
    void convertToLocalDateViaInstantWithUtilDate() {
        Instant nowInstant = Instant.now();
        LocalDate now = LocalDate.ofInstant(nowInstant, ZoneId.systemDefault());
        LocalDate result = DateConvertUtils.convertToLocalDateViaInstant(java.util.Date.from(nowInstant));
        assertEquals(now, result);
    }

    @Test
    void convertToLocalDateViaInstantWithSqlDate() {
        LocalDate now = LocalDate.now();
        LocalDate result = DateConvertUtils.convertToLocalDateViaInstant(java.sql.Date.valueOf(now));
        assertEquals(now, result);
    }

}