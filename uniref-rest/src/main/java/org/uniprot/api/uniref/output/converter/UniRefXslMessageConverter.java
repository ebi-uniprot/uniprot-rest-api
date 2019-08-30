package org.uniprot.api.uniref.output.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXslMessegerConverter;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.uniref.request.UniRefRequest;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryMap;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.domain.Field;
import org.uniprot.store.search.field.UniParcResultFields;

/**
 *
 * @author jluo
 * @date: 22 Aug 2019
 *
 */

public class UniRefXslMessageConverter extends AbstractXslMessegerConverter<UniRefEntry> {
	private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

	public UniRefXslMessageConverter() {
        super(UniRefEntry.class);
    }

	@Override
	protected void initBefore(MessageConverterContext<UniRefEntry> context) {
		tlFields.set(OutputFieldsParser.parse(context.getFields(), UniRefRequest.DEFAULT_FIELDS));
	}

	@Override
	protected List<String> getHeader() {
		List<String> fields = tlFields.get();
		return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());

	}

	@Override
	protected List<String> entry2TsvStrings(UniRefEntry entity) {
		return new UniRefEntryMap(entity, tlFields.get()).getData();
	}

	private String getFieldDisplayName(String field) {
		Optional<Field> opField = UniParcResultFields.INSTANCE.getField(field);
		return opField.map(Field::getLabel).orElse(field);	
	}
}
