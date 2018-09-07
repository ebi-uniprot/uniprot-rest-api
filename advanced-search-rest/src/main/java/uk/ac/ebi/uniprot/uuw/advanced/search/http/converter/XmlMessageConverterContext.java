package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created 06/09/18
 *
 * @author Edd
 */
@Data
@Builder
public class XmlMessageConverterContext<S, T> {
    private String header;
    private String footer;
    private String context;
    private Stream<Collection<S>> entities;
    private Function<S, T> converter;
    private boolean compressed;
}
