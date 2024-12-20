package org.uniprot.api.support.data.statistics.repository;

import java.util.Date;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uniprot.api.support.data.statistics.entity.EntryType;
import org.uniprot.api.support.data.statistics.entity.UniProtRelease;

@Repository
@Primary
public interface UniProtReleaseRepository extends JpaRepository<UniProtRelease, Integer> {
    @Query("SELECT MAX (ur.date) from UniProtRelease ur where ur.name<?1")
    Optional<Date> findPreviousReleaseDate(String currentRelease);

    Optional<UniProtRelease> findByNameAndEntryType(String name, EntryType entryType);
}
