package uk.ac.ebi.uniprot.api.uniparc.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.parser.fasta.UniParcFastaParser;

/**
 *
 * @author jluo
 * @date: 25 Jun 2019
 *
*/

public class UniParcFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniParcEntry> {
	 public UniParcFastaMessageConverter() {
	        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntry.class);
	    }
	
	@Override
	protected void writeEntity(UniParcEntry entity, OutputStream outputStream) throws IOException {
		   outputStream.write((UniParcFastaParser.toFasta(entity) + "\n").getBytes());
	}

}

