package org.uniprot.api.unisave.controller;
//
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import org.uniprot.api.unisave.model.AccessionStatus;
// import org.uniprot.api.unisave.model.EntryInfo;
// import org.uniprot.api.unisave.model.FullEntry;
//
// import java.util.List;
//
// import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
// import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;
//

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.unisave.model.AccessionStatus;
import org.uniprot.api.unisave.model.EntryInfo;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.request.UniSaveRequest;
import org.uniprot.api.unisave.service.UniSaveService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FF_MEDIA_TYPE_VALUE;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@RestController
@RequestMapping("/unisave")
@Slf4j
public class UniSaveController {
    private final MessageConverterContextFactory<UniSaveEntry> converterContextFactory;
    private final UniSaveService service;

    @Autowired
    public UniSaveController(
            MessageConverterContextFactory<UniSaveEntry> converterContextFactory,
            UniSaveService service) {
        this.converterContextFactory = converterContextFactory;
        this.service = service;
    }

    @GetMapping(
            value = "/{accession}",
            produces = {APPLICATION_JSON_VALUE, FASTA_MEDIA_TYPE_VALUE, FF_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getEntries(
            @Valid UniSaveRequest.Entries uniSaveRequest, HttpServletRequest servletRequest) {

        HttpHeaders httpHeaders =
                addDownloadHeaderIfRequired(
                        uniSaveRequest,
                        UniProtMediaType.valueOf(servletRequest.getHeader(HttpHeaders.ACCEPT)),
                        servletRequest);
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(servletRequest.getHeader(HttpHeaders.ACCEPT)));
        context.setEntities(service.getEntries(uniSaveRequest).stream());

        return ResponseEntity.ok().headers(httpHeaders).body(context);
    }

    @GetMapping(
            value = "/{accession}/diff",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getDiff(
            @Valid UniSaveRequest.Diff unisaveRequest, HttpServletRequest servletRequest) {
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(servletRequest.getHeader(HttpHeaders.ACCEPT)));
        context.setEntityOnly(true);
        context.setEntities(
                Stream.of(
                        service.getDiff2(
                                unisaveRequest.getAccession(),
                                unisaveRequest.getVersion1(),
                                unisaveRequest.getVersion2())));

        return ResponseEntity.ok(context);
    }

    @GetMapping(
            value = "/{accession}/status",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<MessageConverterContext<UniSaveEntry>> getStatus(
            @PathVariable String accession, HttpServletRequest servletRequest) {
        MessageConverterContext<UniSaveEntry> context =
                converterContextFactory.get(
                        MessageConverterContextFactory.Resource.UNISAVE,
                        UniProtMediaType.valueOf(servletRequest.getHeader(HttpHeaders.ACCEPT)));
        context.setEntityOnly(true);
        context.setEntities(Stream.of(service.getAccessionStatus2(accession)));

        return ResponseEntity.ok(context);
    }

    private HttpHeaders addDownloadHeaderIfRequired(
            UniSaveRequest.Entries request,
            MediaType contentType,
            HttpServletRequest servletRequest) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (request.isDownload()) {
            String queryString = servletRequest.getQueryString();
            String suffix = "." + UniProtMediaType.getFileExtension(contentType);
            httpHeaders.setContentDispositionFormData(
                    "attachment", "unisave-entries-" + queryString + suffix);
            // used so that gate-way caching uses accept/accept-encoding headers as a key
            httpHeaders.add(VARY, ACCEPT);
            httpHeaders.add(VARY, ACCEPT_ENCODING);
        }
        return httpHeaders;
    }

    //
    //    // ---------------------
    //    // get("/json/status/:acc", operation(getAccessionStatus)){ EntryStatus <- status/acc
    // (json)
    @GetMapping(
            value = "/json/status/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Optional<AccessionStatus>> getEntryStatus(
            @PathVariable String accession) {
        return ResponseEntity.ok(service.getAccessionStatus(accession));
    }
    //
    //    // get("/json/entryinfo/:acc/:ver", operation(findEntryInfoByAccessionAndVersion) -
    @GetMapping(
            value = "/json/entryinfo/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Optional<EntryInfo>> getEntryInfoWithVersion(
            @PathVariable String accession, @PathVariable int version) {
        return ResponseEntity.ok(service.getEntryInfoWithVersion(accession, version));
    }
    //
    //    // get("/json/entryinfos/:acc", List<Entry> <- entries/acc -> all
    @GetMapping(
            value = "/entryinfos/{accession}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EntryInfo>> getEntryInfos(@PathVariable String accession) {
        return ResponseEntity.ok(service.getEntryInfos(accession));
    }
    //
    //    // get("/json/entry/:acc/:ver", operation(findEntryByAccessionAndVersion) -
    @GetMapping(
            value = "/entry/{accession}/{version}",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Optional<UniSaveEntry>> getEntryVersionAsJSON(
            @PathVariable String accession, @PathVariable int version) {
        return ResponseEntity.ok(service.getEntryWithVersion(accession, version));
    }

    //
    //    // get("/json/entries/:acc", operation(findEntriesByAccession) -
    //    @GetMapping(
    //            value = "/entries/{accession}",
    //            produces = {APPLICATION_JSON_VALUE})
    //    public ResponseEntity<List<FullEntry>> getAllEntriesAsJSON(@PathVariable String accession)
    // {
    //        return service.getEntries(accession);
    //    }
    //
    //    // get("/raw/:acc", operation(findRawEntryByAccession) Entry <-
    //    @GetMapping(
    //            value = "/raw/{accession}",
    //            produces = {APPLICATION_JSON_VALUE})
    //    public ResponseEntity<String> getAllEntryVersions(
    //            @PathVariable String accession) {
    //        return service.getFFEntry(accession);
    //    }
    //
    //    // get("/raw/:acc/:ver", operation(findRawEntryByAccessionAndVersion) Entry <-
    //    @GetMapping(
    //            value = "/raw/{accession}/{version}",
    //            produces = {APPLICATION_JSON_VALUE})
    //    public ResponseEntity<String> getEntryVersion(
    //            @PathVariable String accession, @PathVariable int version) {
    //        return service.getFFEntry(accession, version);
    //    }
    //
    //    // get("/raws/:acc/:verlist", operation(findRawEntriesByAccessionAndVersionList)
    //    @GetMapping(
    //            value = "/raws/{accession}/{versionList}",
    //            produces = {APPLICATION_JSON_VALUE})
    //    public ResponseEntity<String> getEntryVersions(
    //            @PathVariable String accession, @PathVariable List<Integer> versionList) {
    //        return service.getFFEntries(accession, versionList); // calls getFFEntry multiple
    // times
    //    }
    //
    //    // get("/raws/:attach/:acc/:verlist")
    //    @GetMapping(
    //            value = "/raws/{ffOrFasta}/{accession}/{versionList}",
    //            produces = {APPLICATION_JSON_VALUE})
    //    public ResponseEntity<String> downloadVersions(
    //            @PathVariable int ffOrFasta, @PathVariable String accession, @PathVariable
    // List<Integer> versionList) {
    //        return service.getEntryWithVersion(accession, versionList); // calls getFFEntry
    // multiple times
    //    }
    //
    //    // get("/json/diff/:acc/:v1/:v2") { Diff <- diff/acc/v1/v2
}
