package com.cloudant.sync.replication;

import com.cloudant.mazha.CouchClient;
import com.cloudant.mazha.DocumentRevs;
import com.cloudant.mazha.HttpRequests;
import com.cloudant.mazha.json.JSONHelper;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.datastore.DocumentRevsList;
import com.cloudant.sync.util.JSONUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Created by ricellis on 11/05/2015.
 */
public class BulkGetRevisionTask implements Callable<List<DocumentRevsList>> {

    private Map<String, Object> bulkRequest = new HashMap<String,Object>();
    private List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
    private final String dbUrl;

    public BulkGetRevisionTask(CouchDB sourceDb, boolean pullAttachmentsInline) {
        try {
            if (sourceDb instanceof CouchClientWrapper) {
                CouchClient client = ((CouchClientWrapper) sourceDb).getCouchClient();
                URI dbUri = client.getRootUri();
                this.dbUrl = dbUri.toURL().toString();
                bulkRequest.put("docs", docs);
            }else{
                throw new UnsupportedOperationException("BulkPull needs access to a DB URL");
            }
        }catch(Exception e) {
            throw new RuntimeException("Error in experimental BulkPullStrategy ", e);
        }
    }

    public void addDocumentToBulkRequest(String id, Collection<String> revisions, Collection<String> ancestors){
        for (String rev : revisions){
            Map<String,Object> docRev = new HashMap<String, Object>();
            docRev.put("id", id);
            docRev.put("rev", rev);
            docRev.put("atts_since", ancestors);
            docs.add(docRev);
        }
    }

    @Override
    public List<DocumentRevsList> call() throws Exception {

        List<DocumentRevs> docRevs = new ArrayList<DocumentRevs>();
        URL bgEndpoint = new URI("").toURL();
        URLConnection urlConn = bgEndpoint.openConnection();
        if (urlConn instanceof HttpsURLConnection){
            HttpsURLConnection conn = (HttpsURLConnection) urlConn;
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            BufferedOutputStream bos = null;
            BufferedInputStream bis = null;
            try {
                conn.connect();
                bos = new BufferedOutputStream(conn.getOutputStream());
                bos.write(JSONUtils.serializeAsBytes(bulkRequest));
                bos.close();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bis = new BufferedInputStream(conn.getInputStream());
                    int i;
                    while ((i = bis.read()) != -1){
                        baos.write(i);
                    }
                    //TODO deconstruct multipart
                    DocumentRevs docRev = JSONUtils.deserialize(baos.toByteArray(), DocumentRevs.class);
                    docRevs.add(docRev);
                }
            }finally{
                if (bos != null) bos.close();
                if (bis != null) bis.close();
                conn.disconnect();
            }

        }
    return Arrays.asList(new DocumentRevsList[]{new DocumentRevsList(docRevs)});
    }

}
