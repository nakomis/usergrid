/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeSeriesCounterSerialization;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;


public class EdgeSeriesCounterSerializationImpl implements EdgeSeriesCounterSerialization {
    @Override
    public MutationBatch writeMetaDataLog( final OrganizationScope scope, final Id nodeId, final long shardId,
                                           final long count, final String... types ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public long getCount( final OrganizationScope scope, final Id nodeId, final long shardId, final String... types ) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}