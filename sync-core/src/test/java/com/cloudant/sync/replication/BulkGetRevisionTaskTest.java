package com.cloudant.sync.replication;

import static org.junit.Assert.assertEquals;

import com.cloudant.mazha.CouchConfig;
import com.cloudant.mazha.DocumentRevs;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

/**
 * Created by ricellis on 19/05/2015.
 */
public class BulkGetRevisionTaskTest {

    @Test
    public void testJsonParse() throws Exception {
        String testJson = "{\"results\":[{\"docs\":[{\"ok\":{\"_id\":\"doc1\",\"_rev\":\"d1r1\"," +
                "\"description\":\"Number 1 test document\",\"name\":\"Test doc 1\"}}]," +
                "\"id\":\"doc1\"},{\"docs\":[{\"ok\":{\"_id\":\"doc2\",\"_rev\":\"d2r1\"," +
                "\"description\":\"Number 2 test document\",\"name\":\"Test doc 2\"}}]," +
                "\"id\":\"doc2\"}]}";

        List<DocumentRevs> docRevs = BulkGetRevisionTask.parseJsonResponse(new ByteArrayInputStream(testJson.getBytes()));
        assertEquals("The DocumentRevs list should have two elements",2, docRevs.size());
    }
}
