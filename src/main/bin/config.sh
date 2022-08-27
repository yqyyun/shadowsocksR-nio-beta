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

constructClassPath() {
  local SHADOWSR_NIO_DIST
  local SHADOWSR_NIO_CLASSPATH

  while read -d '' -r jarfile; do
      if [[ "$jarfile" =~ .*/shadowsocksR-nio[^/]*.jar$ ]]; then
        SHADOWSR_NIO_DIST="$SHADOWSR_NIO_DIST":"$jarfile"
      elif [[ "$SHADOWSR_NIO_CLASSPATH" == "" ]]; then
          SHADOWSR_NIO_CLASSPATH="$jarfile";
      else
          SHADOWSR_NIO_CLASSPATH="$SHADOWSR_NIO_CLASSPATH":"$jarfile"
      fi
  done < <(find "$SHADOWSR_NIO_LIB_DIR" ! -type d -name '*.jar' -print0 | sort -z)
  if [[ "$SHADOWSR_NIO_DIST" == "" ]]; then
      (>&2 echo "[ERROR] ShadowsocksR-nio distribution jar not found in $SHADOWSR_NIO_LIB_DIR.")
      exit 1
  fi

  echo "$SHADOWSR_NIO_CLASSPATH""$SHADOWSR_NIO_DIST"
}

bin=`dirname "$target"`

. "$bin"/env.sh

SYMLINK_RESOLVED_BIN=`cd "$bin"; pwd -P`
SHADOWSR_NIO_HOME=`dirname "$SYMLINK_RESOLVED_BIN"`
SHADOWSR_NIO_LIB_DIR=$SHADOWSR_NIO_HOME/lib
SHADOWSR_NIO_CONF_DIR=$SHADOWSR_NIO_HOME/conf
SHADOWSR_LOG_DIR=$SHADOWSR_NIO_HOME/logs

if [[ "$SHADOW_CONF_DIR" == "" ]]; then
    SHADOW_CONF_DIR=$SHADOWSR_NIO_CONF_DIR
fi

export SHADOWSR_NIO_HOME
export SHADOWSR_NIO_LIB_DIR
export SHADOWSR_NIO_CONF_DIR
export SHADOW_CONF_DIR
export SHADOWSR_LOG_DIR