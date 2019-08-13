package org.uniprot.api.uniparc.output.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.uniparc.request.UniParcRequest;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryMap;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.domain.Field;
import org.uniprot.store.search.field.UniParcResultFields;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcTsvMessageConverter extends AbstractTsvMessagerConverter<UniParcEntry> {
	private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

	public UniParcTsvMessageConverter() {
		super(UniParcEntry.class);
	}

	@Override
	protected void initBefore(MessageConverterContext<UniParcEntry> context) {
		tlFields.set(OutputFieldsParser.parse(context.getFields(), UniParcRequest.DEFAULT_FIELDS));

	}

	@Override
	protected List<String> getHeader() {
		List<String> fields = tlFields.get();
		return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());

	}

	@Override
	protected List<String> entry2TsvStrings(UniParcEntry entity) {
		return new UniParcEntryMap(entity, tlFields.get()).getData();
	}

	private String getFieldDisplayName(String field) {
		Optional<Field> opField = UniParcResultFields.INSTANCE.getField(field);
		if (opField.isPresent())
			return opField.get().getLabel();
		else
			return field;
	}

}
