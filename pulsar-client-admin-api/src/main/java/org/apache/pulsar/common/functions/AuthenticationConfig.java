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
import lombok.NoArgsConstructor;

/**
 * Configuration to aggregate various authentication params.
 * 用于聚合各种身份认证参数的配置
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationConfig {
    /**
     * 客户端身份认证插件
     */
    private String clientAuthenticationPlugin;
    /**
     * 客户端身份认证参数列表
     */
    private String clientAuthenticationParameters;
    // TLS
    /**
     * TLS认证证书文件路径
     */
    private String tlsTrustCertsFilePath;
    /**
     * 使用TLS？
     */
    private boolean useTls;
    /**
     * 允许非安全连接？
     */
    private boolean tlsAllowInsecureConnection;
    /**
     * 启用主机名称验证？
     */
    private boolean tlsHostnameVerificationEnable;
}
