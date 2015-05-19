package com.cloudant.sync.replication;

import com.cloudant.http.HttpConnection;
import com.cloudant.mazha.CouchClient;
import com.cloudant.mazha.DocumentRevs;
import com.cloudant.sync.datastore.DocumentRevsList;
import com.cloudant.sync.util.JSONUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by ricellis on 11/05/2015.
 */
public class BulkGetRevisionTask implements Callable<List<DocumentRevsList>> {

    public static final JsonFactory JSON_FACTORY = new MappingJsonFactory();

    private Map<String, Object> bulkRequest = new HashMap<String, Object>();
    private List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
    private final String dbUrl;

    public BulkGetRevisionTask(CouchDB sourceDb, boolean pullAttachmentsInline) {
        try {
            if (sourceDb instanceof CouchClientWrapper) {
                CouchClient client = ((CouchClientWrapper) sourceDb).getCouchClient();
                URI dbUri = client.getRootUri();
                this.dbUrl = dbUri.toURL().toString();
                bulkRequest.put("docs", docs);
            } else {
                throw new UnsupportedOperationException("BulkPull needs access to a DB URL");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in experimental BulkPullStrategy ", e);
        }
    }

    public void addDocumentToBulkRequest(String id, Collection<String> revisions,
                                         Collection<String> ancestors) {
        for (String rev : revisions) {
            Map<String, Object> docRev = new HashMap<String, Object>();
            docRev.put("id", id);
            docRev.put("rev", rev);
            docRev.put("atts_since", ancestors);
            docs.add(docRev);
        }
    }

    @Override
    public List<DocumentRevsList> call() throws Exception {
        URL bgEndpoint = new URI("").toURL();
        HttpConnection conn = new HttpConnection("POST", bgEndpoint, "application/json");
        conn.setRequestBody(JSONUtils.serializeAsBytes(bulkRequest));
        HttpURLConnection httpConn = conn.execute().getConnection();
        InputStream responseStream = conn.responseAsInputStream();

        try {
            if (HttpURLConnection.HTTP_OK == httpConn.getResponseCode()) {
                DocumentRevsList docRevsList = new DocumentRevsList(parseJsonResponse
                        (responseStream));
                return Arrays.asList(docRevsList);
            } else {
                throw new Exception("Error received from endpoint " + httpConn.getResponseCode()
                        + " " + httpConn.getResponseMessage());
            }
        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    static List<DocumentRevs> parseJsonResponse(InputStream responseStream) throws IOException {
        final List<DocumentRevs> docRevs = new ArrayList<DocumentRevs>();
        JsonParser parser = JSON_FACTORY.createParser(responseStream);
        if (JsonToken.START_OBJECT == parser.nextToken()
                && JsonToken.FIELD_NAME == parser.nextToken()
                && parser.getCurrentName().equals("results")
                && JsonToken.START_ARRAY == parser.nextToken()) {
            while (JsonToken.END_ARRAY != parser.nextToken()) {
                //expect only objects here
                if (JsonToken.START_OBJECT == parser.getCurrentToken()) {
                    //can be docs array or id
                    //fast forward to the docs array
                    while (JsonToken.FIELD_NAME == parser.nextToken() && !parser.getCurrentName()
                            .equals("docs")) {
                        parser.nextToken();
                    }
                    if (JsonToken.START_ARRAY == parser.nextToken()) {
                        while (JsonToken.END_ARRAY != parser.nextToken()) {
                            if (JsonToken.START_OBJECT == parser.getCurrentToken()
                                    && JsonToken.FIELD_NAME == parser.nextToken()) {
                                if (parser.getCurrentName().equals("ok") && JsonToken
                                        .START_OBJECT == parser.nextToken()) {
                                    //read the DocumentRevs and add it to the list
                                    docRevs.add(parser.readValueAs(DocumentRevs.class));
                                } else if (parser.getCurrentName().equals("error")) {
                                    //TODO error case
                                } else {
                                    //TODO not ok or error
                                }
                            }
                        }
                    } else {
                        //TODO error not an array of docs
                    }
                } else {
                    //TODO error not an object in results array
                }
            }
        } else {
            //TODO error not a results array
        }

        return docRevs;
    }
}
