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
package org.apache.pulsar.common.functions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.pulsar.client.api.CompressionType;

/**
 * Configuration of the producer inside the function.
 * 轻量级计算内的生产者配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProducerConfig {
    /**
     * 最大待处理消息数
     */
    private Integer maxPendingMessages;
    /**
     * 跨分区的最大待处理消息数
     */
    private Integer maxPendingMessagesAcrossPartitions;
    /**
     * 使用线程本地生产者？
     */
    private Boolean useThreadLocalProducers;
    /**
     * 轻量级计算内的生产者加密配置
     */
    private CryptoConfig cryptoConfig;
    /**
     * 批量构建者
     */
    private String batchBuilder;
    /**
     * 压缩类型
     */
    private CompressionType compressionType;
}
