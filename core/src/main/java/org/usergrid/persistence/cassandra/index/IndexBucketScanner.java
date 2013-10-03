/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.cassandra.index;

import com.yammer.metrics.annotation.Metered;
import me.prettyprint.hector.api.beans.HColumn;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.cassandra.ApplicationCF;
import org.usergrid.persistence.cassandra.CassandraService;

import java.nio.ByteBuffer;
import java.util.*;

import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;

/**
 * A simple class to make working with index buckets easier. Scans all buckets
 * and merges the results into a single column list to allow easy backwards
 * compatibility with existing code
 * 
 * @author tnine
 * 
 */
public class IndexBucketScanner implements IndexScanner {

  private final CassandraService cass;
  private final IndexBucketLocator indexBucketLocator;
  private final UUID applicationId;
  private final Object keyPrefix;
  private final ApplicationCF columnFamily;
  private final Object finish;
  private final boolean reversed;
  private final int pageSize;
  private final String[] indexPath;
  private final IndexType indexType;

  /**
   * Pointer to our next start read
   */
  private Object start;
  
  /**
   * Set to the original value to start scanning from
   */
  private Object scanStart;

  /**
   * Iterator for our results from the last page load
   */
  private TreeSet<HColumn<ByteBuffer, ByteBuffer>> lastResults;

  /**
   * True if our last load loaded a full page size.
   */
  private boolean hasMore = true;
  

  public IndexBucketScanner(CassandraService cass, IndexBucketLocator locator, ApplicationCF columnFamily,
      UUID applicationId, IndexType indexType, Object keyPrefix, Object start, Object finish, boolean reversed,
      int pageSize, String... indexPath) {
    this.cass = cass;
    this.indexBucketLocator = locator;
    this.applicationId = applicationId;
    this.keyPrefix = keyPrefix;
    this.columnFamily = columnFamily;
    this.start = start;
    this.finish = finish;
    this.reversed = reversed;
    this.pageSize = pageSize;
    this.indexPath = indexPath;
    this.indexType = indexType;
    this.scanStart = start;

  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.cassandra.index.IndexScanner#reset()
   */
  @Override
  public void reset() {
    hasMore = true;
    start = scanStart;
  }

  /**
   * Search the collection index using all the buckets for the given collection.
   * Load the next page. Return false if nothing was loaded, true otherwise
   * 
   * @param indexKey
   * @param slice
   * @param count
   * @param collectionName
   * @return
   * @throws Exception
   */

  public boolean load() throws Exception {

    // nothing left to load
    if (!hasMore) {
      return false;
    }

    List<String> keys = indexBucketLocator.getBuckets(applicationId, indexType, indexPath);

    List<Object> cassKeys = new ArrayList<Object>(keys.size());

    for (String bucket : keys) {
      cassKeys.add(key(keyPrefix, bucket));
    }
    
    //if we skip the first we need to set the load to page size +2, since we'll discard the first
    //and start paging at the next entity, otherwise we'll just load the page size we need 
    int selectSize = pageSize+1;

    TreeSet<HColumn<ByteBuffer,ByteBuffer>> resultsTree = IndexMultiBucketSetLoader.load(cass, columnFamily, applicationId, cassKeys, start, finish, selectSize, reversed);
    
    // we loaded a full page, there might be more
    if (resultsTree.size() == selectSize) {
      hasMore = true;

      // set the bytebuffer for the next pass
      start = resultsTree.last().getName();
      
      resultsTree.remove(resultsTree.last());

    } else {
      hasMore = false;
    }
    
    lastResults = resultsTree;

    return lastResults != null && lastResults.size() > 0;

  }

 

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Set<HColumn<ByteBuffer, ByteBuffer>>> iterator() {
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {

    // We've either 1) paged everything we should and have 1 left from our
    // "next page" pointer
    // Our currently buffered results don't exist or don't have a next. Try to
    // load them again if they're less than the page size
    if (lastResults == null && hasMore) {
      try {
        return load();
      } catch (Exception e) {
        throw new RuntimeException("Error loading next page of indexbucket scanner", e);
      }
    }

    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @Override
  @Metered(group = "core", name = "IndexBucketScanner_load")
  public NavigableSet<HColumn<ByteBuffer, ByteBuffer>> next() {
    NavigableSet<HColumn<ByteBuffer, ByteBuffer>> returnVal = lastResults;

    lastResults = null;

    return returnVal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("You can't remove from a result set, only advance");
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.cassandra.index.IndexScanner#getPageSize()
   */
  @Override
  public int getPageSize() {
    return pageSize;
  }
}
