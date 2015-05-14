/**
 * Copyright (c) 2013 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.sync.replication;

import com.cloudant.mazha.ChangesResult;
import com.cloudant.mazha.CouchConfig;
import com.cloudant.mazha.DocumentRevs;
import com.cloudant.sync.datastore.Attachment;
import com.cloudant.sync.datastore.DatastoreException;
import com.cloudant.sync.datastore.DatastoreExtended;
import com.cloudant.sync.datastore.BasicDocumentRevision;
import com.cloudant.sync.datastore.DocumentException;
import com.cloudant.sync.datastore.DocumentNotFoundException;
import com.cloudant.sync.datastore.DocumentRevsList;
import com.cloudant.sync.datastore.PreparedAttachment;
import com.cloudant.sync.datastore.UnsavedStreamAttachment;
import com.cloudant.sync.util.JSONUtils;
import com.cloudant.sync.util.Misc;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;

import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class BulkPullStrategy extends BasicPullStrategy {

    public BulkPullStrategy(PullReplication pullReplication) {
        super(pullReplication);
    }

    public BulkPullStrategy(PullReplication pullReplication, ExecutorService executorService, PullConfiguration config) {
        super(pullReplication, executorService, config);
    }

    @Override
    public List<Callable<List<DocumentRevsList>>> createTasks(List<String> ids,
                                                        Map<String, Collection<String>> revisions) {

        List<Callable<List<DocumentRevsList>>> tasks = new ArrayList<Callable<List<DocumentRevsList>>>(1);
        BulkGetRevisionTask bulkTask = new BulkGetRevisionTask(sourceDb,config.pullAttachmentsInline);
        tasks.add(bulkTask);
        for(String id : ids) {
            //get the possible ancestors
            Set<String> possibleAncestors = getPossibleAncestors(id, revisions.get(id));
            bulkTask.addDocumentToBulkRequest(id, revisions.get(id), possibleAncestors);
        }
        return tasks;
    }

}
