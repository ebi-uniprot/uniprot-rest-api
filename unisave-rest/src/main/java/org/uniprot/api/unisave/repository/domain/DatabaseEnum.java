package org.uniprot.api.unisave.repository.domain;

public enum DatabaseEnum {
	Swissprot {
        @Override
        public String toString() {
            return "Swiss-Prot";
        }
    }, Trembl {
        @Override
        public String toString() {
            return "TrEMBL";
        }
    }, WrongTrembl {
        @Override
        public String toString() {
            return "TrEMBL-Depreciated";
        }
    };

	public static DatabaseEnum parse(String s) {
		if (s.equalsIgnoreCase("Swiss-Prot") || s.equalsIgnoreCase("Swissprot")) {
			return DatabaseEnum.Swissprot;
		} else if (s.equalsIgnoreCase("Trembl")) {
			return DatabaseEnum.Trembl;
		} else
			throw new IllegalArgumentException(s);
	}

}
