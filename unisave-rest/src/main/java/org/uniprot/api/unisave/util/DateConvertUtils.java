package org.uniprot.api.unisave.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateConvertUtils {

    public static LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        if(dateToConvert == null){
            throw new IllegalArgumentException("Date to convert can not be null");
        }
        if (dateToConvert instanceof java.sql.Date) {
            return ((java.sql.Date) dateToConvert).toLocalDate();
        } else {
            return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }
}
