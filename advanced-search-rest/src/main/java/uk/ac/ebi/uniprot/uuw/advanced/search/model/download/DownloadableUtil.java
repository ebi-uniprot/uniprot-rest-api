package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Evidence;

public class DownloadableUtil {
	public static String evidenceToString(Evidence evidence) {
		StringBuilder sb = new StringBuilder();
		sb.append(evidence.getCode());
		if( (evidence.getSource() != null) && !Strings.isNullOrEmpty(evidence.getSource().getName())) {
			sb.append("|");
			sb.append(evidence.getSource().getName());
			if (!Strings.isNullOrEmpty(evidence.getSource().getId())) {
				sb.append(":").append(evidence.getSource().getId());
			}
		}
		return sb.toString();
	}
	public static String evidencesToString(List<Evidence> evidences) {
		if((evidences ==null) || evidences.isEmpty())
			return "";
		return evidences.stream().map(DownloadableUtil::evidenceToString).collect(Collectors.joining(", ", "{", "}"));
	}
	public static String convertEvidencedString(EvidencedString val) {
		StringBuilder sb = new StringBuilder();
		sb.append(val.getValue());		
		String evStr =DownloadableUtil.evidencesToString(val.getEvidences());
		if(!Strings.isNullOrEmpty(evStr)) {
			sb.append(" ").append(evStr);
		}
		return sb.toString();
	}
}
