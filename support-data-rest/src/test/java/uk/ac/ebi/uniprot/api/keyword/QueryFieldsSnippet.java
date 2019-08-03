package uk.ac.ebi.uniprot.api.keyword;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.restdocs.snippet.TemplatedSnippet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class QueryFieldsSnippet extends TemplatedSnippet {
    private final String filePath;

    public QueryFieldsSnippet(String filePath) {

        super("allowed-fields-in-search-query",null);

        this.filePath = filePath;
    }


    public static Snippet getQueryFieldSnippet(String filePath) {
        return new QueryFieldsSnippet(filePath);
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        try {
            String queryFieldDetail = readFile(this.filePath);
            JSONObject jsonObject = new JSONObject(queryFieldDetail);
            Map<String, Object> model = getModelFromJson(jsonObject);
            return model;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readFile(String filePath) throws IOException {
        File file = new File(filePath);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    private Map<String, Object> getModelFromJson(JSONObject json) throws JSONException {

        Map<String, Object> out = new HashMap<>();

        for (String key : json.keySet()) {
            if (json.get(key) instanceof JSONArray) {
                // Copy an array
                JSONArray arrayIn = json.getJSONArray(key);
                List<Object> arrayOut = new ArrayList<>();
                for (int i = 0; i < arrayIn.length(); i++) {
                    JSONObject item = (JSONObject) arrayIn.get(i);
                    Map<String, Object> items = getModelFromJson(item);
                    arrayOut.add(items);
                }
                out.put(key, arrayOut);
            } else {
                // Copy a primitive string
                out.put(key, json.getString(key));
            }
        }

        return out;
    }
}
