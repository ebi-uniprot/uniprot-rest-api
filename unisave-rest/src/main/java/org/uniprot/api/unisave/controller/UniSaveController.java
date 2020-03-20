package org.uniprot.api.unisave.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.unisave.model.AccessionStatus;
import org.uniprot.api.unisave.model.EntryInfo;
import org.uniprot.api.unisave.model.FullEntry;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@RestController
@RequestMapping("/unisave")
public class UniSaveController {
    /**
     * get("/json/status/:acc", operation(getAccessionStatus)){ EntryStatus <- status/acc (json)
     * get("/json/entryinfo/:acc/:ver", operation(findEntryInfoByAccessionAndVersion) -
     * get("/json/entryinfos/:acc", List<Entry> <- entries/acc -> all get("/json/entry/:acc/:ver",
     * operation(findEntryByAccessionAndVersion) - get("/json/entries/:acc",
     * operation(findEntriesByAccession) - get("/raw/:acc", operation(findRawEntryByAccession) Entry
     * <- get("/raw/:acc/:ver", operation(findRawEntryByAccessionAndVersion) Entry <-
     * get("/raws/:acc/:verlist", operation(findRawEntriesByAccessionAndVersionList)
     * get("/raws/:attach/:acc/:verlist") { List<Entry> <- download/entries/acc/csv_versions
     * get("/json/diff/:acc/:v1/:v2") { Diff <- diff/acc/v1/v2
     *
     * <p>entry/acc?includeFFWithJSON=true (json, flatfile)
     *
     * <p>entry/acc/version?includeFFWithJSON=true (json, flatfile, fasta)
     *
     * <p>versions?includeFFWithJSON=true (json, flatfile, fasta)
     *
     * <p>List<Entry> <- entries/acc/csv_versions?includeFFWithJSON=true -> only specified versions
     * (json, flatfile, fasta)
     *
     * <p>(fasta, flatfile)
     *
     * <p>====================== notes ==============
     * http://www.ebi.ac.uk/uniprot/unisave/rest/json/entryinfo/Q00001/1
     * http://www.ebi.ac.uk/uniprot/unisave/rest/json/entry/Q00001/1
     *
     * <p>entryinfo is the same as entry, but without content
     *
     * <p>============================================== looks like we can reduce end-points, by
     * using content type instead of raw/json
     *
     * <p>here are hte underlying calls to the service: cat
     * /home/edd/working/intellij/unisave/unp.fw.unisave_web/restful/src/main/scala/uk/ac/ebi/unisave/UnisaveServlet.scala
     * | grep -o 'service\.[a-zA-Z]\+' |sort -u
     * service.adaptFasta
     * service.getAccessionStatus
     * service.getDiff
     * service.getEntries
     * service.getEntryInfos
     * service.getEntryInfoWithVersion
     * service.getEntryWithVersion
     */
    private final UniSaveService service;

    @Autowired
    public UniSaveController(UniSaveService service) {
        this.service = service;
    }

    // ---------------------
    // get("/json/status/:acc", operation(getAccessionStatus)){ EntryStatus <- status/acc (json)
    @GetMapping(
            value = "/json/status/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<AccessionStatus> getEntryStatus(
            @PathVariable String accession, @PathVariable int version) {
        return service.getEntryInfoWithVersion(accession, version);
    }

    // get("/json/entryinfo/:acc/:ver", operation(findEntryInfoByAccessionAndVersion) -
    @GetMapping(
            value = "/json/entryinfo/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<EntryInfo> getEntryInfoWithVersion(
            @PathVariable String accession, @PathVariable int version) {
        return service.getEntryInfoWithVersion(accession, version);
    }

    // get("/json/entryinfos/:acc", List<Entry> <- entries/acc -> all
    @GetMapping(
            value = "/entryinfos/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EntryInfo>> getEntryInfos(
            @PathVariable String accession) {
        return service.getEntryInfos(accession);
    }

    // get("/json/entry/:acc/:ver", operation(findEntryByAccessionAndVersion) -
    @GetMapping(
            value = "/entry/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<FullEntry> getEntryVersionAsJSON(
            @PathVariable String accession, @PathVariable int version) {
        return service.getEntryWithVersion(accession, version);
    }

    // get("/json/entries/:acc", operation(findEntriesByAccession) -
    @GetMapping(
            value = "/entries/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<FullEntry>> getAllEntriesAsJSON(@PathVariable String accession) {
        return service.getEntries(accession);
    }

    // get("/raw/:acc", operation(findRawEntryByAccession) Entry <-
    @GetMapping(
            value = "/raw/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getAllEntryVersions(
            @PathVariable String accession) {
        return service.getFFEntry(accession);
    }

    // get("/raw/:acc/:ver", operation(findRawEntryByAccessionAndVersion) Entry <-
    @GetMapping(
            value = "/raw/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getEntryVersion(
            @PathVariable String accession, @PathVariable int version) {
        return service.getFFEntry(accession, version);
    }

    // get("/raws/:acc/:verlist", operation(findRawEntriesByAccessionAndVersionList)
    @GetMapping(
            value = "/raws/{accession}/{versionList}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getEntryVersions(
            @PathVariable String accession, @PathVariable List<Integer> versionList) {
        return service.getFFEntries(accession, versionList); // calls getFFEntry multiple times
    }

    // get("/raws/:attach/:acc/:verlist")
    @GetMapping(
            value = "/raws/{ffOrFasta}/{accession}/{versionList}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> downloadVersions(
            @PathVariable int ffOrFasta, @PathVariable String accession, @PathVariable List<Integer> versionList) {
        return service.getEntryWithVersion(accession, versionList); // calls getFFEntry multiple times
    }

    // get("/json/diff/:acc/:v1/:v2") { Diff <- diff/acc/v1/v2
    @GetMapping(
            value = "/json/diff/{accession}/{version1}/{version2}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getDiffBetween(
            @PathVariable String accession,
            @PathVariable int version1,
            @PathVariable int version2) {
        return service.getDiffBetween(accession, version1 ,version2);
    }
}
