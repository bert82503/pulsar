/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.common.partition;

import java.util.Map;

/**
 * Metadata of a partitioned topic.
 * 分区主题的元数据
 */
public class PartitionedTopicMetadata {

    /**
     * 媒介类型
     */
    public static final String MEDIA_TYPE = "application/vnd.partitioned-topic-metadata+json";

    /**
     * Number of partitions for the topic
     * 主题的分区数量
     */
    public int partitions;
    /**
     * 删除标识
     */
    public boolean deleted;

    /**
     * Topic properties
     * 主题的属性集
     */
    public Map<String, String> properties;

    public PartitionedTopicMetadata() {
        // 非分区主题
        this(0);
    }

    public PartitionedTopicMetadata(int partitions) {
        this.partitions = partitions;
        this.properties = null;
    }

    public PartitionedTopicMetadata(int partitions, Map<String, String> properties) {
        this.partitions = partitions;
        this.properties = properties;
    }

    /**
     * A topic with '0' partitions is treated like non-partitioned topic.
     * 非分区主题
     */
    public static final int NON_PARTITIONED = 0;

}
