package org.uniprot.api.uniprotkb.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;

@Service
public class UniProtKBEntryVersionService {

    private final UniSaveClient uniSaveClient;

    public UniProtKBEntryVersionService(UniSaveClient uniSaveClient) {
        this.uniSaveClient = uniSaveClient;
    }

    public String getEntryVersion(String version, String accession) {
        String entryVersion = null;
        if (version.equals("last")) {
            try {
                String response = uniSaveClient.getUniSaveHistoryVersion(accession);
                JSONObject jsonObject = new JSONObject(response);
                if (!jsonObject.has("messages")) {
                    entryVersion =
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
            entryVersion = version;
        }

        return entryVersion;
    }
}
