package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Gene;

public class DownloadableGene implements Downloadable {
	private static final String SPACE = " ";
	private static final String SEMICOLON = "; ";
	private final List<Gene> genes;
	public static final List<String> FIELDS = 
			Arrays.asList(
					"gene_names", "gene_primary",
					"gene_synonym", "gene_oln",
					"gene_orf"
			);
	

	public DownloadableGene(List<Gene> genes) {
		if(genes ==null) {
			this.genes = Collections.emptyList();
		}else
			this.genes = Collections.unmodifiableList(genes);
	}

	@Override
	public Map<String, String> map() {
		if(genes.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), getGeneName());
		map.put(FIELDS.get(1), getPrimaryName());
		map.put(FIELDS.get(2), getSynonyms());
		map.put(FIELDS.get(3), getOlnName());
		map.put(FIELDS.get(4), getOrfName());

		return map;
	}

	public String getGeneName() {
		if (genes.isEmpty()) {
			return "";
		}
		return genes.stream().map(val -> getGeneName(val)).collect(Collectors.joining(SEMICOLON));
	}

	private String getGeneName(Gene gene) {
		StringBuilder sb = new StringBuilder();
		if (gene.getName() != null) {
			sb.append(gene.getName().getValue());
		}
		String synonym = joinNames(gene.getSynonyms());
		String olnName = joinNames(gene.getOlnNames());
		String orfName = joinNames(gene.getOrfNames());
		append(sb, synonym);
		append(sb, olnName);
		append(sb, orfName);
		return sb.toString();
	}

	private void append(StringBuilder sb, String name) {
		if (!Strings.isNullOrEmpty(name)) {
			if (sb.length() > 0) {
				sb.append(SPACE);
			}
			sb.append(name);
		}
	}

	public String getPrimaryName() {
		if (genes.isEmpty()) {
			return "";
		}
		return genes.stream().map(val -> val.getName()==null? "":val.getName().getValue()).collect(Collectors.joining(SEMICOLON));
	}

	public String getSynonyms() {
		if (genes.isEmpty()) {
			return "";
		}
		return genes.stream().map(val -> joinNames(val.getSynonyms())).collect(Collectors.joining(SEMICOLON));

	}

	public String getOlnName() {
		if (genes.isEmpty()) {
			return "";
		}
		return genes.stream().map(val -> joinNames(val.getOlnNames())).collect(Collectors.joining(SEMICOLON));

	}

	public String getOrfName() {
		if (genes.isEmpty()) {
			return "";
		}
		return genes.stream().map(val -> joinNames(val.getOrfNames())).collect(Collectors.joining(SEMICOLON));

	}

	private String joinNames(List<EvidencedString> names) {
		if ((names != null) && !names.isEmpty()) {
			return names.stream().map(val -> val.getValue()).collect(Collectors.joining(SPACE));
		} else
			return "";
	}
	public static  boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));
		
	}
}
