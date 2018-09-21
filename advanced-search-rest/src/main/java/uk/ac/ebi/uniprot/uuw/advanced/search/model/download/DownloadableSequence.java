package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Sequence;

public class DownloadableSequence implements Downloadable {
	public static final List<String> FIELDS = 
			Arrays.asList(new String [] {
					"sequence", "sequence_version",
					"mass", "length", "date_seq_mod", "fragment"
			});
	
	private final Sequence sequence;
	public DownloadableSequence(Sequence sequence) {
		this.sequence =sequence;
	}
	
	@Override
	public Map<String, String> getData() {
		 Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), sequence.getSequence()==null?"": sequence.getSequence());
		map.put(FIELDS.get(1), ""+sequence.getVersion());
		map.put(FIELDS.get(2), ""+sequence.getMass());
		map.put(FIELDS.get(3), "" + sequence.getLength());
		map.put(FIELDS.get(4), sequence.getModified()==null?"": sequence.getModified());
		map.put(FIELDS.get(5), sequence.getFragment()==null?"": sequence.getFragment());
		 return map;
	}

}
