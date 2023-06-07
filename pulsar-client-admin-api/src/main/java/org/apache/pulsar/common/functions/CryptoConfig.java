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

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.ProducerCryptoFailureAction;

/**
 * Configuration of the producer inside the function.
 * 轻量级计算内的生产者加密配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CryptoConfig {
    /**
     * 加密密钥读取器的类名称
     */
    private String cryptoKeyReaderClassName;
    /**
     * 加密密钥读取器的配置集
     */
    private Map<String, Object> cryptoKeyReaderConfig;

    /**
     * 加密密钥列表
     */
    private String[] encryptionKeys;

    // 失败操作

    /**
     * 生产者加密的失败操作
     */
    private ProducerCryptoFailureAction producerCryptoFailureAction;

    /**
     * 消费者加密的失败操作
     */
    private ConsumerCryptoFailureAction consumerCryptoFailureAction;
}
