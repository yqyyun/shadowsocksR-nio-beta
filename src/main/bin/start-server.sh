#!/usr/bin/env bash

#
# Copyright 2022 yqy
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

target="$0"

iteration=0
while [ -L "$target" ]; do
    if [ "$iteration" -gt 100 ]; then
        echo "Cannot resolve path: You have a cyclic symlink in $target."
        break
    fi
    ls=`ls -ld -- "$target"`
    target=`expr "$ls" : '.* -> \(.*\)$'`
    iteration=$((iteration + 1))
done

bin=`dirname "$target"`

. "$bin"/config.sh

if [ "$SHADOW_CONF_DIR" = "" ]; then
    SHADOW_CONF_DIR=`cd "$bin";cd ../conf;pwd`
fi

CC_CLASSPATH=`constructClassPath`

if [[ "$JAVA_HOME" != "" ]]; then
    JAVA_RUN=$JAVA_HOME/bin/java
else
    JAVA_RUN=java
fi

if ! type java > /dev/null 2> /dev/null ; then
    echo "Please specify JAVA_HOME. Either in env.sh or as system-wide JAVA_HOME."
    exit 1
fi
#log=$SHADOWSR_LOG_DIR/shadowsocks4-$SHADOWSOCKSR_IDENT_STRING-server-$HOSTNAME.log
#log=$SHADOWSR_LOG_DIR/shadowsocksr-nio-server.log
log=$SHADOWSR_LOG_DIR
log_setting=(-DlogPath="$log" -Dlog4j2.configurationFile=file:"$SHADOW_CONF_DIR/log4j2.xml")

exec $JAVA_RUN $JVM_ARGS "${log_setting[@]}" -classpath "$CC_CLASSPATH" com.yqy.Server "$@" > /dev/null 2>&1 &