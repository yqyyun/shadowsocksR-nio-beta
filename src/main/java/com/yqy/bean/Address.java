/*
 * Copyright 2022 yqy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yqy.bean;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author yqy
 * @date 2022/8/7 07:43
 */
public class Address {

    public final byte[] address;

    public final int port;

    public final int atype;

    public Address(byte[] address, int port, int atype) {
        this.address = address;
        this.port = port;
        this.atype = atype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address1 = (Address) o;
        return port == address1.port && atype == address1.atype && Arrays.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(port, atype);
        result = 31 * result + Arrays.hashCode(address);
        return result;
    }
}
