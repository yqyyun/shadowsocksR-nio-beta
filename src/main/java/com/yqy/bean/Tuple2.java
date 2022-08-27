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

/**
 * @author yqy
 * @date 2022/8/6 11:35
 */
public class Tuple2<T1, T2> {
    public final T1 f1;
    public final T2 f2;

    public Tuple2(T1 f1, T2 f2) {
        this.f1 = f1;
        this.f2 = f2;
    }

    public static <U1, U2> Tuple2<U1, U2> of(U1 u1, U2 u2) {
        return new Tuple2<>(u1, u2);
    }
}
