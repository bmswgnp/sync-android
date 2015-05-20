package com.cloudant.sync.replication;

import com.cloudant.http.HttpConnection;
import com.cloudant.mazha.DocumentRevs;
import com.cloudant.sync.datastore.DocumentRevsList;
import com.cloudant.sync.util.JSONUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class BulkGetRevisionTask implements Callable<List<DocumentRevsList>> {

    public static final JsonFactory JSON_FACTORY = new MappingJsonFactory();

    private static final String PROXY_URL;

    static {
        //check to see if a proxy has been set
        PROXY_URL = System.getProperty("cloudant.db.proxy");
    }

    private Map<String, Object> bulkRequest = new HashMap<String, Object>();
    private List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
    private final URL dbUrl;
    private final boolean pullAttachmentsInline;

    public BulkGetRevisionTask(URI dbUri, boolean pullAttachmentsInline) {
        try {
            this.dbUrl = dbUri.toURL();
            bulkRequest.put("docs", docs);
            this.pullAttachmentsInline = pullAttachmentsInline;
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
        //use the proxy url if set, otherwise use the db
        String dbLocation = (PROXY_URL == null) ? dbUrl.toString() : PROXY_URL;
        URL bgEndpoint = new URI(dbLocation + "/_bulk_get?attachments=" +
                pullAttachmentsInline + "&revs=true").toURL();
        HttpConnection conn = new HttpConnection("POST", bgEndpoint, "application/json");

        //if we are using a proxy then we supply the DB as X-Cloudant-URL
        if (PROXY_URL != null) {
            conn.requestProperties.put("X-Cloudant-URL", dbUrl.toString());
        }

        //put the json in the request
        conn.setRequestBody(JSONUtils.serializeAsBytes(bulkRequest));
        HttpURLConnection httpConn = conn.execute().getConnection();
        InputStream responseStream = conn.responseAsInputStream();

        try {
            if (HttpURLConnection.HTTP_OK == httpConn.getResponseCode()) {
                return parseJsonResponse
                        (responseStream);
            } else {
                throw new Exception("Error received from endpoint " + httpConn.getResponseCode()
                        + " " + httpConn.getResponseMessage());
            }
        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    static List<DocumentRevsList> parseJsonResponse(InputStream responseStream) throws IOException {
        final Map<String, List<DocumentRevs>> docRevsById = new HashMap<String,
                List<DocumentRevs>>();
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
                                    //read the DocumentRevs and add to the list for this id
                                    DocumentRevs revs = parser.readValueAs(DocumentRevs.class);
                                    List<DocumentRevs> docRevs = docRevsById.get(revs.getId());
                                    if (docRevs == null) {
                                        docRevsById.put(revs.getId(), (docRevs = new
                                                ArrayList<DocumentRevs>()));
                                    }
                                    docRevs.add(revs);
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

        List<DocumentRevsList> allDocRevs = new ArrayList<DocumentRevsList>();
        for (List<DocumentRevs> docRevs : docRevsById.values()) {
            allDocRevs.add(new DocumentRevsList(docRevs));
        }

        return allDocRevs;
    }
}
