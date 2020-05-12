package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;
import org.uniprot.api.uniprotkb.model.converter.EvidenceConverter;
import org.uniprot.core.uniprotkb.evidence.EvidenceDatabaseTypes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class EvidencedString {
    private String value;
    private List<Evidence> evidences;

    public static  EvidencedString convert(String val, List<EvidenceId> evsIds){
        return new EvidencedString(val,
                                   evsIds.stream().map(ConverterHelper::convertEvidence)
                                           .collect(Collectors.toList())
        );
    }

    public static  EvidencedString convert(EvidencedValue data){
        return convert(data.getValue(), data.getEvidenceIds());
    }

    public static EvidencedString convertEvidence(String value, List<org.uniprot.core.uniprotkb.evidence.Evidence> evidences) {
        return EvidencedString.builder()
                .value(value)
                .evidences(evidences.stream().map(e -> EvidenceConverter.convertEvidence(e, null)).collect(Collectors.toList()))
                .build();
    }

    public static org.uniprot.core.uniprotkb.evidence.Evidence convertEvidence(Evidence t){
        return FeatureJsonConverterImpl.convertEvidence(t, null);
    }

    public static Evidence convertEvidence(Evidence ev, String acc) {
        EvidenceDatabaseTypes.INSTANCE.getType(ev.toString())
        EvidenceTypeMapping instance = EvidenceTypeMapping.getInstance();
        String ecoCode = ev.getEvidenceCode().getCodeValue();

        Evidence featureEvidence = new Evidence();
        featureEvidence.setCode(ecoCode);
        featureEvidence.setSource(instance.map(ev, acc));

        return featureEvidence;
    }
}
