package org.uniprot.api.rest.output.context;

import java.util.function.Function;

/**
 * Created 22/10/18
 *
 * @author Edd
 */
interface ConverterContext<S, T> {
    Function<S, T> getConverter();

    void setConverter(Function<S, T> converter);
}
