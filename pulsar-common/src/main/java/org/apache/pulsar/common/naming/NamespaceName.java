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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Parser of a value from the namespace field provided in configuration.
 * 配置中提供的命名空间字段的值的解析器。
 */
public class NamespaceName implements ServiceUnitId {

    /**
     * 完整的命名空间身份
     */
    private final String namespace;

    /**
     * 租户身份
     */
    private final String tenant;
    /**
     * 集群身份
     */
    private final String cluster;
    /**
     * 命名空间的本地名称
     */
    private final String localName;

    /**
     * 异步加载的本地缓存
     * <pre>
     * 命名空间的名称规格：<tenant>/<namespace> 或 <tenant>/<cluster>/<namespace>
     * 数据模型：{@code <"tenant/namespace", NamespaceName>}
     * </pre>
     */
    private static final LoadingCache<String, NamespaceName> cache = CacheBuilder.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, NamespaceName>() {
                @Override
                public NamespaceName load(String name) throws Exception {
                    return new NamespaceName(name);
                }
            });

    // 命名空间

    /**
     * 系统侧的命名空间
     */
    public static final NamespaceName SYSTEM_NAMESPACE = NamespaceName.get("pulsar/system");

    public static NamespaceName get(String tenant, String namespace) {
        validateNamespaceName(tenant, namespace);
        return get(tenant + '/' + namespace);
    }

    public static NamespaceName get(String tenant, String cluster, String namespace) {
        validateNamespaceName(tenant, cluster, namespace);
        return get(tenant + '/' + cluster + '/' + namespace);
    }

    public static NamespaceName get(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Invalid null namespace: " + namespace);
        }
        try {
            // 从本地缓存获取
            return cache.get(namespace);
        } catch (ExecutionException | UncheckedExecutionException e) {
            // 抛出异常根因
            throw (RuntimeException) e.getCause();
        }
    }

    public static Optional<NamespaceName> getIfValid(String namespace) {
        // 直接从本地缓存读取数据
        NamespaceName ns = cache.getIfPresent(namespace);
        if (ns != null) {
            return Optional.of(ns);
        }

        if (namespace.length() == 0) {
            return Optional.empty();
        }

        // Example: my-tenant/my-namespace
        if (!namespace.contains("/")) {
            return Optional.empty();
        }

        return Optional.of(get(namespace));
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private NamespaceName(String namespace) {
        // Verify it's a proper namespace
        // The namespace name is composed of <tenant>/<namespace>
        // or in the legacy format with the cluster name:
        // <tenant>/<cluster>/<namespace>
        try {
            // 命名空间的名称规格
            String[] parts = namespace.split("/");
            if (parts.length == 2) {
                // 新样式的命名空间
                // New style namespace : <tenant>/<namespace>
                validateNamespaceName(parts[0], parts[1]);

                tenant = parts[0];
                cluster = null;
                localName = parts[1];
            } else if (parts.length == 3) {
                // Old style namespace: <tenant>/<cluster>/<namespace>
                validateNamespaceName(parts[0], parts[1], parts[2]);

                tenant = parts[0];
                cluster = parts[1];
                localName = parts[2];
            } else {
                throw new IllegalArgumentException("Invalid namespace format. namespace: " + namespace);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid namespace format."
                    + " expected <tenant>/<namespace> or <tenant>/<cluster>/<namespace> "
                    + "but got: " + namespace, e);
        }
        this.namespace = namespace;
    }

    public String getTenant() {
        return tenant;
    }

    @Deprecated
    public String getCluster() {
        return cluster;
    }

    public String getLocalName() {
        return localName;
    }

    public boolean isGlobal() {
        // 全局集群
        return cluster == null || Constants.GLOBAL_CLUSTER.equalsIgnoreCase(cluster);
    }

    // 主题名称

    public String getPersistentTopicName(String localTopic) {
        // 持久化的主题名称
        return getTopicName(TopicDomain.persistent, localTopic);
    }

    /**
     * Compose the topic name from namespace + topic.
     * 根据"命名空间+主题"组合主题名称
     *
     * @param domain 主题域
     * @param topic  主题
     * @return 主题名称
     */
    String getTopicName(TopicDomain domain, String topic) {
        if (domain == null) {
            throw new IllegalArgumentException("invalid null domain");
        }
        NamedEntity.checkName(topic);
        // 主题名称规格：<TopicDomain>://<namespace>/<topic>
        return String.format("%s://%s/%s", domain.toString(), namespace, topic);
    }

    @Override
    public String toString() {
        // 完整的命名空间
        return namespace;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NamespaceName) {
            NamespaceName other = (NamespaceName) obj;
            return Objects.equals(namespace, other.namespace);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return namespace.hashCode();
    }

    public static void validateNamespaceName(String tenant, String namespace) {
        if ((tenant == null || tenant.isEmpty())
                || (namespace == null || namespace.isEmpty())) {
            throw new IllegalArgumentException(
                    String.format("Invalid namespace format. namespace: %s/%s", tenant, namespace));
        }
        NamedEntity.checkName(tenant);
        NamedEntity.checkName(namespace);
    }

    public static void validateNamespaceName(String tenant, String cluster, String namespace) {
        if ((tenant == null || tenant.isEmpty())
                || (cluster == null || cluster.isEmpty())
                || (namespace == null || namespace.isEmpty())) {
            throw new IllegalArgumentException(
                    String.format("Invalid namespace format. namespace: %s/%s/%s", tenant, cluster, namespace));
        }
        NamedEntity.checkName(tenant);
        NamedEntity.checkName(cluster);
        NamedEntity.checkName(namespace);
    }

    @Override
    public NamespaceName getNamespaceObject() {
        return this;
    }

    @Override
    public boolean includes(TopicName topicName) {
        return this.equals(topicName.getNamespaceObject());
    }

    /**
     * Returns true if this is a V2 namespace prop/namespace-name.
     *
     * @return true if v2
     */
    public boolean isV2() {
        return cluster == null;
    }
}
