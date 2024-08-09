package org.uniprot.api.support.data.statistics.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uniprot.api.support.data.statistics.entity.UniProtRelease;

@Repository
@Primary
public interface UniProtReleaseRepository extends JpaRepository<UniProtRelease, String> {}
