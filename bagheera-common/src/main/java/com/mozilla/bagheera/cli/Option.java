/*
 * Copyright 2012 Mozilla Foundation
 *
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mozilla.bagheera.cli;

/**
 * This Option class extends Apache Commons every so slightly to make required options
 * more convienent to setup.
 */
public class Option extends org.apache.commons.cli.Option {

    private static final long serialVersionUID = 4764162254116593978L;

    public Option(String opt, String longOpt, boolean hasArg, String description) {
        this(opt, longOpt, hasArg, description, false);
    }
    
    public Option(String opt, String longOpt, boolean hasArg, String description, boolean required) {
        super(opt, longOpt, hasArg, description);
        setRequired(required);
    }
    
    public Option required() {
        setRequired(true);
        return this;
    }
}
