package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.StopStreamException;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */

public class ProteomeJsonMessageConverter extends AbstractEntityHttpMessageConverter<ProteomeEntry> {
	private final ObjectMapper objectMapper = ProteomeJsonConfig.getInstance().getDefaultFullObjectMapper();
	private ThreadLocal<JsonGenerator> tlJsonGenerator = new ThreadLocal<>();

	public ProteomeJsonMessageConverter() {
		super(MediaType.APPLICATION_JSON);
	}

	@Override
	protected void before(MessageConverterContext<ProteomeEntry> context, OutputStream outputStream) throws IOException {
		JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);

		if (!context.isEntityOnly()) {
			generator.writeStartObject();

			if (context.getFacets() != null) {
				generator.writeFieldName("facets");
				generator.writeStartArray();
				context.getFacets().forEach(facet -> writeObject(generator, facet));
				generator.writeEndArray();
			}

			generator.writeFieldName("results");
			generator.writeStartArray();
		}

		tlJsonGenerator.set(generator);
	}

	private void writeObject(JsonGenerator generator, Object facet) {
		try {
			generator.writeObject(facet);
		} catch (IOException e) {
			throw new StopStreamException("Failed to write Facet JSON object", e);
		}
	}

	@Override
	protected void writeEntity(ProteomeEntry entity, OutputStream outputStream) throws IOException {
		JsonGenerator generator = tlJsonGenerator.get();
		generator.writeObject(entity);

	}

	@Override
	protected void after(MessageConverterContext<ProteomeEntry> context, OutputStream outputStream) throws IOException {
		JsonGenerator generator = tlJsonGenerator.get();

		if (!context.isEntityOnly()) {
			generator.writeEndArray();
			generator.writeEndObject();
		}
		generator.flush();
		generator.close();
	}
}
