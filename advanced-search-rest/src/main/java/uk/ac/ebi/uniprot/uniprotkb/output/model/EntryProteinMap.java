package uk.ac.ebi.uniprot.uniprotkb.output.model;

import com.google.common.base.Strings;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Protein;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.ProteinName;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.ProteinName.Name;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class EntryProteinMap implements NamedValueMap {
    private static final String EC2 = "EC";
    private static final String SQUARE_BLACKET_RIGHT = "]";
    private static final String SQUARE_BLACKET_LEFT = "[";
    private static final String SEMICOLON = "; ";
    private static final String CLEAVED_INTO = "Cleaved into:";
    private static final String INCLUDES = "Includes:";
    private static final String CD_ANTIGEN = "CD antigen";
    private static final String BIOTECH = "biotech";
    private static final String ALLERGEN = "allergen";
    private static final String BLACKET_RIGHT = ")";
    private static final String BLACKET_LEFT = "(";
    private static final String SPACE = " ";
    private static final String DELIMITER = ", ";

    public static final List<String> FIELDS = Arrays.asList(
            "protein_name", "ec"
    );

    private final Protein protein;

    public EntryProteinMap(Protein protein) {
        this.protein = protein;
    }

    @Override
    public Map<String, String> attributeValues() {
        if (protein == null) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        map.put(FIELDS.get(0), getProteinName());
        String ecs = getECNumber();
        if (!Strings.isNullOrEmpty(ecs)) {
            map.put(FIELDS.get(1), ecs);
        }
        return map;
    }

    private String getProteinName() {
        StringBuilder sb = new StringBuilder();
        if (protein.getRecommendedName() != null) {
            sb.append(getDownloadStringFromName(protein.getRecommendedName()));
        }
        if ((protein.getAlternativeName() != null) && !protein.getAlternativeName().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(SPACE);
            }
            sb.append(protein.getAlternativeName().stream()
                              .map(val -> BLACKET_LEFT + getDownloadStringFromName(val) + BLACKET_RIGHT)
                              .collect(Collectors.joining(SPACE)));
        }
        if ((protein.getSubmittedName() != null) && !protein.getSubmittedName().isEmpty()) {
            sb.append(getDownloadStringFromName(protein.getSubmittedName().get(0)));
            String data = protein.getSubmittedName().stream().skip(1)
                    .map(val -> BLACKET_LEFT + getDownloadStringFromName(val) + BLACKET_RIGHT)
                    .collect(Collectors.joining(SPACE));
            if (!Strings.isNullOrEmpty(data)) {
                sb.append(SPACE).append(data);
            }
        }
        if (protein.getAllergenName() != null) {
            sb.append(SPACE).append(BLACKET_LEFT).append(ALLERGEN).append(SPACE)
                    .append(protein.getAllergenName().getValue()).append(BLACKET_RIGHT);
        }
        if (protein.getBiotechName() != null) {
            sb.append(SPACE).append(BLACKET_LEFT).append(BIOTECH).append(SPACE)
                    .append(protein.getBiotechName().getValue()).append(BLACKET_RIGHT);
        }
        if ((protein.getCdAntigenName() != null) && !protein.getCdAntigenName().isEmpty()) {
            sb.append(SPACE);
            sb.append(protein.getCdAntigenName().stream()
                              .map(val -> BLACKET_LEFT + CD_ANTIGEN + SPACE + val.getValue() + BLACKET_RIGHT)
                              .collect(Collectors.joining(SPACE)));
        }
        if ((protein.getInnName() != null) && !protein.getInnName().isEmpty()) {
            sb.append(SPACE);
            sb.append(protein.getInnName().stream().map(val -> BLACKET_LEFT + val.getValue() + BLACKET_RIGHT)
                              .collect(Collectors.joining(SPACE)));
        }
        if ((protein.getComponent() != null) && !protein.getComponent().isEmpty()) {
            sb.append(SPACE).append(SQUARE_BLACKET_LEFT).append(CLEAVED_INTO).append(SPACE)
                    .append(protein.getComponent().stream().map(this::getDownloadStringFromProteinName)
                                    .collect(Collectors.joining(SEMICOLON)))
                    .append(SPACE).append(SQUARE_BLACKET_RIGHT);
        }

        if ((protein.getDomain() != null) && !protein.getDomain().isEmpty()) {
            sb.append(SPACE).append(SQUARE_BLACKET_LEFT).append(INCLUDES).append(SPACE).append(protein.getDomain()
                                                                                                       .stream()
                                                                                                       .map(this::getDownloadStringFromProteinName)
                                                                                                       .collect(Collectors
                                                                                                                        .joining(SEMICOLON)))
                    .append(SPACE).append(SQUARE_BLACKET_RIGHT);
        }
        return sb.toString();
    }

    private String getECNumber() {
        Set<String> ecs = new TreeSet<>();
        if (protein.getRecommendedName() != null) {
            ecs.addAll(getEcFromName(protein.getRecommendedName()));
        }
        if ((protein.getAlternativeName() != null) && !protein.getAlternativeName().isEmpty()) {
            protein.getAlternativeName().forEach(val -> ecs.addAll(getEcFromName(val)));
        }
        if ((protein.getSubmittedName() != null) && !protein.getSubmittedName().isEmpty()) {
            protein.getSubmittedName().forEach(val -> ecs.addAll(getEcFromName(val)));

        }

        if ((protein.getComponent() != null) && !protein.getComponent().isEmpty()) {
            protein.getComponent().stream().forEach(val -> ecs.addAll(getEcFromProteinName(val)));
        }

        if ((protein.getDomain() != null) && !protein.getDomain().isEmpty()) {
            protein.getDomain().stream().forEach(val -> ecs.addAll(getEcFromProteinName(val)));
        }
        if (ecs.isEmpty()) {
            return "";
        } else {
            return String.join("; ", ecs);
        }
    }

    private String getDownloadStringFromProteinName(ProteinName pname) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDownloadStringFromName(pname.getRecommendedName()));
        if ((pname.getAlternativeName() != null) && !pname.getAlternativeName().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(SPACE);
            }
            sb.append(pname.getAlternativeName().stream()
                              .map(val -> BLACKET_LEFT + getDownloadStringFromName(val) + BLACKET_RIGHT)
                              .collect(Collectors.joining(SPACE)));
        }

        return sb.toString();
    }

    private String getDownloadStringFromName(Name name) {
        StringBuilder sb = new StringBuilder();
        sb.append(name.getFullName().getValue());
        String sname = name.getShortName().stream().map(EvidencedString::getValue)
                .collect(Collectors.joining(DELIMITER));
        String ec = name.getEcNumber().stream().map(val -> EC2 + SPACE + val.getValue())
                .collect(Collectors.joining(DELIMITER));
        if (!Strings.isNullOrEmpty(sname)) {
            sb.append(DELIMITER).append(sname);
        }
        if (!Strings.isNullOrEmpty(ec)) {
            sb.append(DELIMITER).append(ec);
        }
        return sb.toString();
    }

    private List<String> getEcFromProteinName(ProteinName pname) {
        List<String> ec = new ArrayList<>();
        ec.addAll(getEcFromName(pname.getRecommendedName()));
        if ((pname.getAlternativeName() != null) && !pname.getAlternativeName().isEmpty()) {
            pname.getAlternativeName().forEach(val -> ec.addAll(getEcFromName(val)));
        }
        return ec;
    }

    private List<String> getEcFromName(Name name) {
        return name.getEcNumber().stream().map(EvidencedString::getValue).collect(Collectors.toList());
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);

    }
}
