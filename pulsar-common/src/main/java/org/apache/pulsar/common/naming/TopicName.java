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
package org.apache.pulsar.common.naming;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.common.util.Codec;

/**
 * Encapsulate the parsing of the completeTopicName name.
 * 封装对完整的主题名称的解析。
 */
public class TopicName implements ServiceUnitId {

    /**
     * 公共的租户
     */
    public static final String PUBLIC_TENANT = "public";
    /**
     * 默认的命名空间
     */
    public static final String DEFAULT_NAMESPACE = "default";

    /**
     * 分区主题的后缀
     */
    public static final String PARTITIONED_TOPIC_SUFFIX = "-partition-";

    /**
     * 完整的主题名称
     */
    private final String completeTopicName;

    /**
     * 主题域
     */
    private final TopicDomain domain;
    /**
     * 租户身份
     */
    private final String tenant;
    /**
     * 集群身份
     */
    private final String cluster;
    /**
     * 命名空间部分
     */
    private final String namespacePortion;
    /**
     * 主题的本地名称
     */
    private final String localName;

    /**
     * 命名空间的名称的解析器
     */
    private final NamespaceName namespaceName;

    /**
     * 分区索引
     */
    private final int partitionIndex;

    /**
     * 异步加载的本地缓存
     * <pre>
     * 主题名称规格：<domain>://<tenant>/<namespace>/<topic>
     * 数据模型：{@code <"domain://tenant/namespace/topic", TopicName>}
     * </pre>
     */
    private static final LoadingCache<String, TopicName> cache = CacheBuilder.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, TopicName>() {
                @Override
                public TopicName load(String name) throws Exception {
                    return new TopicName(name);
                }
            });

    // 主题名称

    public static TopicName get(String domain, NamespaceName namespaceName, String topic) {
        // 主题名称规格：<domain>://<namespace>/<topic>
        String name = domain + "://" + namespaceName.toString() + '/' + topic;
        return TopicName.get(name);
    }

    public static TopicName get(String domain, String tenant, String namespace, String topic) {
        // 主题名称规格：<domain>://<tenant>/<namespace>/<topic>
        String name = domain + "://" + tenant + '/' + namespace + '/' + topic;
        return TopicName.get(name);
    }

    public static TopicName get(
            String domain, String tenant, String cluster, String namespace,
            String topic) {
        String name = domain + "://" + tenant + '/' + cluster + '/' + namespace + '/' + topic;
        return TopicName.get(name);
    }

    public static TopicName get(String topic) {
        try {
            // 从本地缓存获取
            return cache.get(topic);
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    public static TopicName getPartitionedTopicName(String topic) {
        TopicName topicName = TopicName.get(topic);
        if (topicName.isPartitioned()) {
            return TopicName.get(topicName.getPartitionedTopicName());
        }
        return topicName;
    }

    public static boolean isValid(String topic) {
        try {
            get(topic);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private TopicName(String completeTopicName) {
        try {
            // The topic name can be in two different forms, one is fully qualified topic name,
            // the other one is short topic name
            if (!completeTopicName.contains("://")) {
                // 简短的主题名称
                // The short topic name can be:
                // - <topic>
                // - <property>/<namespace>/<topic>
                String[] parts = StringUtils.split(completeTopicName, '/');
                if (parts.length == 3) {
                    // 完整的主题名称
                    completeTopicName = TopicDomain.persistent.name() + "://" + completeTopicName;
                } else if (parts.length == 1) {
                    completeTopicName = TopicDomain.persistent.name() + "://"
                        + PUBLIC_TENANT + "/" + DEFAULT_NAMESPACE + "/" + parts[0];
                } else {
                    throw new IllegalArgumentException(
                        "Invalid short topic name '" + completeTopicName + "', it should be in the format of "
                        + "<tenant>/<namespace>/<topic> or <topic>");
                }
            }

            // 完全限定的主题名称
            // The fully qualified topic name can be in two different forms:
            // new:    persistent://tenant/namespace/topic
            // legacy: persistent://tenant/cluster/namespace/topic

            List<String> parts = Splitter.on("://").limit(2).splitToList(completeTopicName);
            this.domain = TopicDomain.getEnum(parts.get(0));

            String rest = parts.get(1);

            // 主题名称的其余部分
            // The rest of the name can be in different forms:
            // new:    tenant/namespace/<localName>
            // legacy: tenant/cluster/namespace/<localName>
            // Examples of localName:
            // 1. some, name, xyz
            // 2. xyz-123, feeder-2


            parts = Splitter.on("/").limit(4).splitToList(rest);
            if (parts.size() == 3) {
                // 新的主题名称
                // New topic name without cluster name
                this.tenant = parts.get(0);
                this.cluster = null;
                this.namespacePortion = parts.get(1);
                this.localName = parts.get(2);
                this.partitionIndex = getPartitionIndex(completeTopicName);
                this.namespaceName = NamespaceName.get(tenant, namespacePortion);
            } else if (parts.size() == 4) {
                // Legacy topic name that includes cluster name
                this.tenant = parts.get(0);
                this.cluster = parts.get(1);
                this.namespacePortion = parts.get(2);
                this.localName = parts.get(3);
                this.partitionIndex = getPartitionIndex(completeTopicName);
                this.namespaceName = NamespaceName.get(tenant, cluster, namespacePortion);
            } else {
                throw new IllegalArgumentException("Invalid topic name: " + completeTopicName);
            }


            if (localName == null || localName.isEmpty()) {
                throw new IllegalArgumentException("Invalid topic name: " + completeTopicName);
            }

        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid topic name: " + completeTopicName, e);
        }
        // 完整的主题名称
        if (isV2()) {
            this.completeTopicName = String.format("%s://%s/%s/%s",
                                                   domain, tenant, namespacePortion, localName);
        } else {
            this.completeTopicName = String.format("%s://%s/%s/%s/%s",
                                                   domain, tenant, cluster,
                                                   namespacePortion, localName);
        }
    }

    public boolean isPersistent() {
        return TopicDomain.persistent == domain;
    }

    // 命名空间

    /**
     * Extract the namespace portion out of a completeTopicName name.
     * 从完整的主题名称中提取命名空间的部分。
     *
     * <p>Works both with old & new convention.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespaceName.toString();
    }

    /**
     * Get the namespace object that this completeTopicName belongs to.
     *
     * @return namespace object
     */
    @Override
    public NamespaceName getNamespaceObject() {
        return namespaceName;
    }

    public TopicDomain getDomain() {
        return domain;
    }

    public String getTenant() {
        return tenant;
    }

    @Deprecated
    public String getCluster() {
        return cluster;
    }

    public String getNamespacePortion() {
        return namespacePortion;
    }

    public String getLocalName() {
        return localName;
    }

    public String getEncodedLocalName() {
        return Codec.encode(localName);
    }

    // 分区主题

    public TopicName getPartition(int index) {
        // 分区索引
        if (index == -1 || this.toString().endsWith(PARTITIONED_TOPIC_SUFFIX + index)) {
            return this;
        }
        // 主题-topic
        // 分区名称规格：<completeTopicName>-partition-<partitionIndex>
        String partitionName = this.toString() + PARTITIONED_TOPIC_SUFFIX + index;
        return get(partitionName);
    }

    /**
     * @return partition index of the completeTopicName.
     * It returns -1 if the completeTopicName (topic) is not partitioned.
     * 完整的主题名称（主题）是未分区
     */
    public int getPartitionIndex() {
        return partitionIndex;
    }

    public boolean isPartitioned() {
        return partitionIndex != -1;
    }

    /**
     * 对于主题中的分区，返回基本的分区主题名称。
     * For partitions in a topic, return the base partitioned topic name.
     * Eg:
     * <ul>
     *  <li><code>persistent://prop/cluster/ns/my-topic-partition-1</code> -->
     *      <code>persistent://prop/cluster/ns/my-topic</code>
     *  <li><code>persistent://prop/cluster/ns/my-topic</code> -->
     *      <code>persistent://prop/cluster/ns/my-topic</code>
     * </ul>
     */
    public String getPartitionedTopicName() {
        if (isPartitioned()) {
            // 分区的主题名称
            return completeTopicName.substring(0, completeTopicName.lastIndexOf("-partition-"));
        } else {
            return completeTopicName;
        }
    }

    /**
     * @return partition index of the completeTopicName.
     * It returns -1 if the completeTopicName (topic) is not partitioned.
     */
    public static int getPartitionIndex(String topic) {
        int partitionIndex = -1;
        if (topic.contains(PARTITIONED_TOPIC_SUFFIX)) {
            try {
                String idx = StringUtils.substringAfterLast(topic, PARTITIONED_TOPIC_SUFFIX);
                partitionIndex = Integer.parseInt(idx);
                if (partitionIndex < 0) {
                    // for the "topic-partition--1"
                    partitionIndex = -1;
                } else if (StringUtils.length(idx) != String.valueOf(partitionIndex).length()) {
                    // for the "topic-partition-01"
                    partitionIndex = -1;
                }
            } catch (NumberFormatException nfe) {
                // ignore exception
            }
        }

        return partitionIndex;
    }

    /**
     * 获取主题的分区名称。
     * A helper method to get a partition name of a topic in String.
     * @return topic + "-partition-" + partition.
     */
    public static String getTopicPartitionNameString(String topic, int partitionIndex) {
        return topic + PARTITIONED_TOPIC_SUFFIX + partitionIndex;
    }

    /**
     * 获取主题的管控请求路径。
     * Returns the http rest path for use in the admin web service.
     * Eg:
     *   * "persistent/my-tenant/my-namespace/my-topic"
     *   * "non-persistent/my-tenant/my-namespace/my-topic"
     *
     * @return topic rest path
     */
    public String getRestPath() {
        return getRestPath(true);
    }

    public String getRestPath(boolean includeDomain) {
        String domainName = includeDomain ? domain + "/" : "";
        if (isV2()) {
            return String.format("%s%s/%s/%s", domainName, tenant, namespacePortion, getEncodedLocalName());
        } else {
            return String.format("%s%s/%s/%s/%s", domainName, tenant, cluster, namespacePortion, getEncodedLocalName());
        }
    }

    /**
     * Returns the name of the persistence resource associated with the completeTopicName.
     * 返回与完整的主题名称关联的持久化资源的名称。
     *
     * @return the relative path to be used in persistence
     */
    public String getPersistenceNamingEncoding() {
        // 协议是 domain://tenant/namespace/topic
        // 持久化顺序是 tenant/namespace/domain/topic
        // The convention is: domain://tenant/namespace/topic
        // We want to persist in the order: tenant/namespace/domain/topic

        // For legacy naming scheme, the convention is: domain://tenant/cluster/namespace/topic
        // We want to persist in the order: tenant/cluster/namespace/domain/topic
        if (isV2()) {
            return String.format("%s/%s/%s/%s", tenant, namespacePortion, domain, getEncodedLocalName());
        } else {
            return String.format("%s/%s/%s/%s/%s", tenant, cluster, namespacePortion, domain, getEncodedLocalName());
        }
    }

    /**
     * Get a string suitable for completeTopicName lookup.
     * 获取适合完整主题名称的查找名称。
     *
     * <p>Example:
     *
     * <p>persistent://tenant/cluster/namespace/completeTopicName ->
     *   persistent/tenant/cluster/namespace/completeTopicName
     *
     * @return 查找名称
     */
    public String getLookupName() {
        if (isV2()) {
            return String.format("%s/%s/%s/%s", domain, tenant, namespacePortion, getEncodedLocalName());
        } else {
            return String.format("%s/%s/%s/%s/%s", domain, tenant, cluster, namespacePortion, getEncodedLocalName());
        }
    }

    public boolean isGlobal() {
        return cluster == null || Constants.GLOBAL_CLUSTER.equalsIgnoreCase(cluster);
    }

    public String getSchemaName() {
        // 模式名称 tenant/namespace/topic
        return getTenant()
            + "/" + getNamespacePortion()
            + "/" + TopicName.get(getPartitionedTopicName()).getEncodedLocalName();
    }

    @Override
    public String toString() {
        // 完整的主题名称
        return completeTopicName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TopicName) {
            TopicName other = (TopicName) obj;
            return Objects.equals(completeTopicName, other.completeTopicName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return completeTopicName.hashCode();
    }

    @Override
    public boolean includes(TopicName otherTopicName) {
        return this.equals(otherTopicName);
    }

    /**
     * Returns true if this a V2 topic name prop/ns/topic-name.
     * @return true if V2
     */
    public boolean isV2() {
        return cluster == null;
    }
}
