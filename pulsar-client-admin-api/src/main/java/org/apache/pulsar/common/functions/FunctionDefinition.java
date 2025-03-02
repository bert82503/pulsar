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

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basic information about a Pulsar function.
 * 轻量级计算定义
 */
@Data
@NoArgsConstructor
public class FunctionDefinition {

    /**
     * The name of the function type.
     * 轻量级计算类型的名称
     */
    private String name;

    /**
     * Description to be used for user help.
     * 用户帮助描述
     */
    private String description;

    /**
     * The class name for the function implementation.
     * 轻量级计算实现类的完整限定类名
     *
     * <p>If not defined, it will be assumed this function cannot act as a data.
     */
    private String functionClass;
}
