package org.uniprot.api.uniprotkb.common.service.ec;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;

@Service
public class ECService {
    private final ECRepo ecRepo;

    public ECService(ECRepo ecRepo) {
        this.ecRepo = ecRepo;
    }

    public Optional<ECEntry> getEC(String fullEc) {
        return ecRepo.getEC(fullEc);
    }
}
