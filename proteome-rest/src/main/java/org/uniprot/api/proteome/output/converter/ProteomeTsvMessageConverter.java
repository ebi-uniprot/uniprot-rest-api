package org.uniprot.api.proteome.output.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.api.configure.proteome.ProteomeResultFields;
import org.uniprot.api.configure.uniprot.domain.Field;
import org.uniprot.api.proteome.request.ProteomeRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.core.parser.tsv.proteome.ProteomeEntryMap;
import org.uniprot.core.proteome.ProteomeEntry;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */

public class ProteomeTsvMessageConverter extends AbstractTsvMessagerConverter<ProteomeEntry> {
	private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

	public ProteomeTsvMessageConverter() {
		super(ProteomeEntry.class);
	}

	@Override
	protected void initBefore(MessageConverterContext<ProteomeEntry> context) {
		tlFields.set(OutputFieldsParser.parse(context.getFields(), ProteomeRequest.DEFAULT_FIELDS));

	}

	@Override
	protected List<String> getHeader() {
		List<String> fields = tlFields.get();
		return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());

	}

	@Override
	protected List<String> entry2TsvStrings(ProteomeEntry entity) {
		return new ProteomeEntryMap(entity, tlFields.get()).getData();
	}

	private String getFieldDisplayName(String field) {
		Optional<Field> opField = ProteomeResultFields.INSTANCE.getField(field);
		if (opField.isPresent())
			return opField.get().getLabel();
		else
			return field;
	}

}
