package org.uniprot.api.unisave.repository.domain;

public enum DatabaseEnum {
    Swissprot {
        @Override
        public String toString() {
            return "Swiss-Prot";
        }
    },
    Trembl {
        @Override
        public String toString() {
            return "TrEMBL";
        }
    },
    WrongTrembl {
        @Override
        public String toString() {
            return "TrEMBL-Depreciated";
        }
    };
}
