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

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Configuration of a consumer.
 * 消费者的配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConsumerConfig {
    /**
     * 协议类型
     */
    private String schemaType;
    private String serdeClassName;
    /**
     * 正则表达式模式匹配？
     */
    private boolean isRegexPattern;
    /**
     * 协议的属性集
     */
    @Builder.Default
    private Map<String, String> schemaProperties = new HashMap<>();
    /**
     * 消费者的属性集
     */
    @Builder.Default
    private Map<String, String> consumerProperties = new HashMap<>();
    /**
     * 接收方的队列大小
     */
    private Integer receiverQueueSize;
    /**
     * 加密配置
     */
    private CryptoConfig cryptoConfig;
    /**
     * 池消息？
     * 批量消息
     */
    private boolean poolMessages;

    public ConsumerConfig(String schemaType) {
        this.schemaType = schemaType;
    }
}
