package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference;

public class DownloadableDbXRef implements Downloadable {
	private static final String DR = "dr:";
	private final List<DbReference> dbReferences;
	private static final Map<String, String > D3MethodMAP= new HashMap<>();
	static {
		D3MethodMAP.put("X-ray", "X-ray crystallography");
		D3MethodMAP.put("NMR", "NMR spectroscopy");
		D3MethodMAP.put("EM", "Electron microscopy");
		D3MethodMAP.put("Model", "Model");
		D3MethodMAP.put("Neutron", "Neutron diffraction");
		D3MethodMAP.put("Fiber", "Fiber diffraction");
		D3MethodMAP.put("IR", "Infrared spectroscopy");
	};

	public static boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> val.startsWith(DR))
				|| DownloadableGoXref.contains(fields);
		
	}
	
	public DownloadableDbXRef(List<DbReference> dbReferences) {
		if (dbReferences == null) {
			this.dbReferences = Collections.emptyList();
		} else {
			this.dbReferences = Collections.unmodifiableList(dbReferences);
		}
	}

	@Override
	public Map<String, String> map() {
		if (dbReferences.isEmpty()) {
			Collections.emptyMap();
		}
		Map<String, String> map = new HashMap<>();
		Map<String, List<DbReference>> xrefMap = dbReferences.stream()
				.collect(Collectors.groupingBy(val -> val.getType()));
		xrefMap.entrySet().stream().forEach(val -> addToMap(map, val.getKey(), val.getValue()));
		return map;
	}

	private void addToMap(Map<String, String> map, String type, List<DbReference> xrefs) {
		DatabaseType dbType = DatabaseType.getDatabaseType(type);
		if (dbType == DatabaseType.GO) {
			DownloadableGoXref dlGoXref = new DownloadableGoXref(xrefs);
			Map<String, String> goMap = dlGoXref.map();
			goMap.entrySet().stream().forEach(val -> map.put(val.getKey(), val.getValue()));
		}else if (dbType == DatabaseType.PROTEOMES) {
			map.put(DR + dbType.name().toLowerCase(),
					xrefs.stream().map(DownloadableDbXRef::proteomeXrefToString).collect(Collectors.joining("; ")));
		} else {
			map.put(DR + dbType.name().toLowerCase(),
					xrefs.stream().map(DownloadableDbXRef::dbXrefToString).collect(Collectors.joining(";", "", ";")));
			if(dbType ==DatabaseType.PDB) {
				map.put("3d", pdbXrefTo3DString(xrefs));
			}
		}
	}

	private String pdbXrefTo3DString(List<DbReference> xrefs) {
		Map<String, Long> result=
		xrefs.stream().flatMap(val -> val.getProperties().stream())
		.filter(val ->val.getType().equalsIgnoreCase("method"))
		.map(val->val.getValue())
		.map(val ->D3MethodMAP.get(val) )
		.filter(val-> val !=null)
		.collect(Collectors.groupingBy(val ->val, TreeMap::new, Collectors.counting()));
		
		return result.entrySet().stream()
		.map(val -> (val.getKey() + " (" + val.getValue().toString() +")"))
		.collect(Collectors.joining("; "));
	}
	
	public static String dbXrefToString(DbReference xref) {
		StringBuilder sb = new StringBuilder();
		sb.append(xref.getId());
		if (!Strings.isNullOrEmpty(xref.getIsoform())) {
			sb.append(" [").append(xref.getIsoform()).append("]");
		}
		return sb.toString();
	}
	
	public static String proteomeXrefToString(DbReference xref) {
	
		StringBuilder sb = new StringBuilder();
		sb.append(xref.getId())
		.append(": ")
		.append(xref.getProperties().get(0).getValue());
	
		return sb.toString();
	}
}
