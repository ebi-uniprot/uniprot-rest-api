package org.uniprot.api.uniprotkb.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.core.util.Utils;

import javax.servlet.http.HttpServletRequest;

@Service
public class UniProtKBEntryVersionService {

    private final UniSaveClient uniSaveClient;

    public UniProtKBEntryVersionService(UniSaveClient uniSaveClient) {
        this.uniSaveClient = uniSaveClient;
    }

    public String getEntryVersion(HttpServletRequest request, String accession) {
        String version = null;
        if (request.getParameter("version").equals("last")) {
            try {
                String response = uniSaveClient.getUniSaveHistoryVersion(accession);
                JSONObject jsonObject = new JSONObject(response);
                if (!jsonObject.has("messages")) {
                    version =
                            jsonObject
                                    .getJSONArray("results")
                                    .getJSONObject(0)
                                    .get("entryVersion")
                                    .toString();
                } else {
                    String errorResponse = (String) ((JSONArray) jsonObject.get("messages")).get(0);
                    throw new ResourceNotFoundException(errorResponse);
                }
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            }
        } else {
            version = request.getParameter("version");
        }

        return version;
    }
}
