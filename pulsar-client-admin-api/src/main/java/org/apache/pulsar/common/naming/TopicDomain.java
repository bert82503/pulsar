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

/**
 * Enumeration showing if a topic is persistent.
 * 主题域是否持久化。
 */
public enum TopicDomain {
    /**
     * 持久化
     */
    persistent("persistent"),
    /**
     * 非持久化
     */
    non_persistent("non-persistent"),
    ;

    private final String value;

    TopicDomain(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static TopicDomain getEnum(String value) {
        for (TopicDomain e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid topic domain: '" + value + "'");
    }

    @Override
    public String toString() {
        return this.value;
    }
}
