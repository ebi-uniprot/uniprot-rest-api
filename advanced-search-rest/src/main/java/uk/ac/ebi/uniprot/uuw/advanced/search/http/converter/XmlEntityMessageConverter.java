package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.xml.jaxb.uniprot.Entry;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created 06/09/18
 *
 * @author Edd
 */

public class XmlEntityMessageConverter {
    private String header;
    private String footer;
    private String context;
    private Stream<Collection<UniProtEntry>> entities;
    private Function<UniProtEntry, Entry> converter;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Stream<Collection<UniProtEntry>> getEntities() {
        return entities;
    }

    public void setEntities(Stream<Collection<UniProtEntry>> entities) {
        this.entities = entities;
    }

    public Function<UniProtEntry, Entry> getConverter() {
        return converter;
    }

    public void setConverter(Function<UniProtEntry, Entry> converter) {
        this.converter = converter;
    }
}
