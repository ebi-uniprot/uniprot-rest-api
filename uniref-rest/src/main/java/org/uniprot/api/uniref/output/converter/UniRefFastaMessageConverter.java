package org.uniprot.api.uniref.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.UniRefFastaParser;
import org.uniprot.core.uniref.UniRefEntry;

/**
 *
 * @author jluo
 * @date: 22 Aug 2019
 *
*/

public class UniRefFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniRefEntry> {

	 public UniRefFastaMessageConverter() {
	        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniRefEntry.class);
	    }
	
	@Override
	protected void writeEntity(UniRefEntry entity, OutputStream outputStream) throws IOException {
		   outputStream.write((UniRefFastaParser.toFasta(entity) + "\n").getBytes());
	}

}

