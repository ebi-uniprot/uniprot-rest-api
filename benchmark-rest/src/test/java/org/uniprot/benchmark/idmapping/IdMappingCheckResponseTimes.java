package org.uniprot.benchmark.idmapping;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.core.util.Utils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Strings;

/**
 * Created 09/03/2021
 *
 * @author Edd
 */
@Slf4j
public class IdMappingCheckResponseTimes {
    private static final HttpHeaders HTTP_HEADERS = new HttpHeaders();
    private final RestTemplate restTemplate =
            new RestTemplate(
                    (new SimpleClientHttpRequestFactory() {
                        protected void prepareConnection(
                                HttpURLConnection connection, String httpMethod)
                                throws IOException {
                            super.prepareConnection(connection, httpMethod);
                            connection.setInstanceFollowRedirects(false);
                        }
                    }));
    private final String from;
    private final String to;
    private final String host;
    private final String ids;

    static {
        HTTP_HEADERS.setContentType(APPLICATION_FORM_URLENCODED);
        HTTP_HEADERS.setAccept(List.of(APPLICATION_JSON));
    }

    private final String idMappingPath;
    private final StopWatch stopWatch;
    private final String resultsParams;
    private final String idFilePath;

    public IdMappingCheckResponseTimes(Parameters parameters) {
        from = parameters.from;
        to = parameters.to;
        host = parameters.host;
        idMappingPath = parameters.idMappingPath;
        idFilePath = parameters.idFile;
        if (Utils.notNullNotEmpty(parameters.ids)) {
            ids = parameters.ids;
        } else {
            ids = initIds();
        }
        stopWatch = new StopWatch();
        resultsParams =
                Utils.notNull(parameters.resultsParams) ? "?" + parameters.resultsParams : "";
    }

    private String initIds() {
        List<String> ids = new ArrayList<>();
        try {
            // try to read file of IDs. If there is a problem with it, read from stdin
            InputStream in = System.in;
            if (Utils.notNullNotEmpty(idFilePath)) {
                in = new FileInputStream(idFilePath);
            }
            try (InputStreamReader streamReader = new InputStreamReader(in);
                    BufferedReader reader = new BufferedReader(streamReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ids.add(line.replace("\n", ""));
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Strings.join(",", ids);
    }

    private void processAll() {
        log.info("===============================");
        log.info("MAPPING REQUEST INFO: STARTING");
        log.info("\tFrom: {}", from);
        log.info("\tTo: {}", to);
        int truncateIdsPos = Math.min(ids.length(), 40);
        log.info("\tIds: {} ...", ids.substring(0, truncateIdsPos));
        log.info("\tId count: {}", ids.chars().filter(c -> c == ',').count() + 1);
        log.info(
                "\tResult request params: {}",
                Utils.notNullNotEmpty(resultsParams) ? resultsParams : "<NONE>");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(runUrl());

        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(), HTTP_HEADERS);

        // ---- SUBMIT JOB ----
        stopWatch.start(builder.toUriString());
        ResponseEntity<JobIdResponse> responseResponseEntity =
                restTemplate.postForEntity(builder.toUriString(), requestBody, JobIdResponse.class);
        stopWatch.stop();

        if (responseResponseEntity.getStatusCode().equals(HttpStatus.OK)) {
            JobIdResponse jobIdResponse = responseResponseEntity.getBody();

            String jobId = Objects.requireNonNull(jobIdResponse).getJobId();

            HttpEntity<Object> httpEntity = new HttpEntity<>(HTTP_HEADERS);
            String statusUrl = statusUrl(jobId);

            // ---- FETCH INITIAL STATUS OF JOB ----
            stopWatch.start("Time spent checking status at: " + statusUrl);
            ResponseEntity<JobStatusResponse> statusEntity =
                    restTemplate.exchange(
                            statusUrl, HttpMethod.GET, httpEntity, JobStatusResponse.class);

            JobStatusResponse statusEntityBody = statusEntity.getBody();
            boolean jobIsFinished =
                    Objects.requireNonNull(statusEntityBody).jobStatus.equals("FINISHED");
            // ... keep checking status until FINISHED
            while (!jobIsFinished) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // ---- FETCH STATUS OF JOB AT LATER TIME ----
                statusEntity =
                        restTemplate.exchange(
                                statusUrl, HttpMethod.GET, httpEntity, JobStatusResponse.class);

                statusEntityBody = statusEntity.getBody();
                jobIsFinished =
                        Objects.requireNonNull(statusEntityBody).jobStatus.equals("FINISHED");
            }
            stopWatch.stop();

            // job is now finished
            HttpHeaders headers = statusEntity.getHeaders();
            URI location = headers.getLocation();
            log.info("Redirect location: {}", location);
            if (Utils.notNull(location)) {
                // ---- FETCH RESULTS OF JOB ----
                String resultsUrl = host + location.toString() + resultsParams;
                stopWatch.start(resultsUrl);
                ResponseEntity<String> responseEntity =
                        restTemplate.exchange(resultsUrl, HttpMethod.GET, httpEntity, String.class);
                log.info("Results headers: {}", responseEntity.getHeaders());
                stopWatch.stop();
            }

        } else {
            System.exit(1);
        }

        log.info("---------- Timing summary ----------");
        log.info("{} seconds <= total time", stopWatch.getTotalTimeSeconds());
        Arrays.stream(stopWatch.getTaskInfo())
                .forEach(
                        task ->
                                log.info(
                                        "{} seconds <= {}",
                                        task.getTimeSeconds(),
                                        task.getTaskName()));
        log.info("--------------------");

        log.info("MAPPING REQUEST INFO: FINISHED");
        log.info("===============================");
    }

    private String idMappingUrl() {
        return host + idMappingPath;
    }

    private String runUrl() {
        return idMappingUrl() + "/run";
    }

    private String statusUrl(String jobId) {
        return idMappingUrl() + "/status/" + jobId;
    }

    @Data
    public static class JobIdResponse {
        private String jobId;
    }

    @Data
    public static class JobStatusResponse {
        private String jobStatus;
    }

    private MultiValueMap<String, String> createPostBody() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", ids);
        map.add("from", from);
        map.add("to", to);

        return map;
    }

    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        JCommander.newBuilder().addObject(parameters).build().parse(args);

        IdMappingCheckResponseTimes checker = new IdMappingCheckResponseTimes(parameters);
        checker.processAll();
    }

    private static class Parameters {
        @Parameter(required = true, names = "-from")
        public String from;

        @Parameter(required = true, names = "-to")
        public String to;

        @Parameter(required = true, names = "-host", description = "E.g., http://localhost:8090")
        public String host;

        @Parameter(required = true, names = "-idMappingPath")
        public String idMappingPath = "/uniprot/api/idmapping";

        @Parameter(required = false, names = "-ids")
        public String ids;

        @Parameter(
                required = false,
                names = "-resultsParams",
                description = "For example, \"cursor=XX&facets=reviewed\"")
        public String resultsParams;

        @Parameter(
                required = false,
                names = "-idFile",
                description = "Path to file containing IDs. One line per ID.")
        public String idFile;
    }
}
