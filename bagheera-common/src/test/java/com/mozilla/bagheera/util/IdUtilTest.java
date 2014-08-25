/*
 * Copyright 2011 Mozilla Foundation
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
package com.mozilla.bagheera.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;

public class IdUtilTest {

    @Test
    public void testBucketizeIdWithTimestamp2() throws IOException {
        UUID uuid = UUID.randomUUID();
        long ts = System.currentTimeMillis();
        byte[] idBytes = IdUtil.bucketizeId(uuid.toString(), ts);
        assertNotNull(idBytes);
        String bucketIdStr = new String(idBytes);
        assertTrue(bucketIdStr.endsWith(IdUtil.SDF.format(new Date(ts)) + uuid.toString()));
    }
    
    @Test
    public void testNonRandByteBucketizeId1() throws IOException {
        boolean caughtException = false;
        try {
            IdUtil.nonRandByteBucketizeId(null, Calendar.getInstance().getTime());
        } catch (IllegalArgumentException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
    }

    @Test
    public void testNonRandByteBucketizeId2() throws IOException {
        boolean caughtException = false;
        try {
            UUID uuid = UUID.randomUUID();
            IdUtil.nonRandByteBucketizeId(uuid.toString(), null);
        } catch (IllegalArgumentException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
    }

    @Test
    public void testNonRandByteBucketizeId3() throws IOException {
        UUID uuid = UUID.randomUUID();
        Date d = Calendar.getInstance().getTime();
        byte[] idBytes = IdUtil.nonRandByteBucketizeId(uuid.toString(), d);
        assertNotNull(idBytes);
        String bucketIdStr = new String(idBytes);
        assert(bucketIdStr.endsWith(IdUtil.SDF.format(d) + uuid.toString()));
    }

    @Test
    public void testNonRandByteBucketizeId4() throws IOException {
        UUID uuid = UUID.randomUUID();
        Date d = Calendar.getInstance().getTime();
        byte[] idBytes1 = IdUtil.nonRandByteBucketizeId(uuid.toString(), d);
        assertNotNull(idBytes1);
        String bucketIdStr1 = new String(idBytes1);

        byte[] idBytes2 = IdUtil.nonRandByteBucketizeId(uuid.toString(), d);
        assertNotNull(idBytes2);
        String bucketIdStr2 = new String(idBytes2);

        assertTrue(bucketIdStr1.equals(bucketIdStr2));
    }

}
