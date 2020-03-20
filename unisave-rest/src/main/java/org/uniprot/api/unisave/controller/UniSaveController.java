package org.uniprot.api.unisave.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.unisave.model.AccessionStatus;
import org.uniprot.api.unisave.model.EntryInfo;
import org.uniprot.api.unisave.model.FullEntry;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@RestController
@RequestMapping("/unisave")
public class UniSaveController {
    /**
     *
     -  get("/json/status/:acc", operation(getAccessionStatus)){
     EntryStatus <- status/acc (json)

     -  get("/raw/:acc", operation(findRawEntryByAccession)
     Entry <- entry/acc?includeFFWithJSON=true (json, flatfile)

     -  get("/json/entry/:acc/:ver", operation(findEntryByAccessionAndVersion)
     -  get("/json/entryinfo/:acc/:ver", operation(findEntryInfoByAccessionAndVersion)
     -  get("/raw/:acc/:ver",   operation(findRawEntryByAccessionAndVersion)
     Entry <- entry/acc/version?includeFFWithJSON=true (json, flatfile, fasta)

     -  get("/json/entries/:acc", operation(findEntriesByAccession)
     -  get("/json/entryinfos/:acc",
     List<Entry> <- entries/acc -> all versions?includeFFWithJSON=true (json, flatfile, fasta)

     -  get("/raws/:acc/:verlist", operation(findRawEntriesByAccessionAndVersionList)
     List<Entry> <- entries/acc/csv_versions?includeFFWithJSON=true -> only specified versions  (json, flatfile, fasta)



     -  get("/raws/:attach/:acc/:verlist") {
     List<Entry> <- download/entries/acc/csv_versions (fasta, flatfile)

     -  get("/json/diff/:acc/:v1/:v2") {
     Diff <- diff/acc/v1/v2

     ====================== notes ==============
     http://www.ebi.ac.uk/uniprot/unisave/rest/json/entryinfo/Q00001/1
     http://www.ebi.ac.uk/uniprot/unisave/rest/json/entry/Q00001/1

     entryinfo is the same as entry, but without content
     */

    private final UniSaveService service;

    @Autowired
    public UniSaveController(UniSaveService service) {
        this.service = service;
    }

    @GetMapping(
            value = "/status/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<AccessionStatus> getAccessionStatus(@PathVariable String accession) {
        return service.getAccessionStatus(accession);
    }

    @GetMapping(
            value = "/entry/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<FullEntry> getEntryWithVersion(
            @PathVariable String accession, @PathVariable int version) {
        return service.getEntryWithVersion(accession, version);
    }

    @GetMapping(
            value = "/entryinfo/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<EntryInfo> getEntryInfoWithVersion(
            @PathVariable String accession, @PathVariable int version) {
        return service.getEntryInfoWithVersion(accession, version);
    }

    //  get("/json/entries/:acc", operation(findEntriesByAccession)
    @GetMapping(
            value = "/entries/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<FullEntry>> getEntriesForAccession(
            @PathVariable String accession) {
        return service.getEntries(accession);
    }

    //  get("/json/entryinfos/:acc",
    @GetMapping(
            value = "/entryinfos/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EntryInfo>> getEntryInfosForAccession(
            @PathVariable String accession) {
        return service.getEntryInfos(accession);
    }

    //  get("/raw/:acc/:ver",   operation(findRawEntryByAccessionAndVersion)
    @GetMapping(
            value = "/raw/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<String> getEntryInfosForAccession(
            @PathVariable String accession, @PathVariable int version) {
        return service.getFFEntry(accession);
    }

    //  get("/raw/:acc", operation(findRawEntryByAccession)
    //  get("/raws/:acc/:verlist", operation(findRawEntriesByAccessionAndVersionList)
    ////  get("/raws/:attach/:acc/:verlist", operation(downloadRawEntriesByAccessionAndVersionList))
    // {
    //  get("/raws/:attach/:acc/:verlist") {
    //    get("/json/diff/:acc/:v1/:v2") {
}
