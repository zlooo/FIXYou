#!/usr/bin/env bash

for i in {1..$1}
do
  echo "Running iteration $i"
  ./gradlew integrationtest --debug >> itLogs.log
  if [ $? != 0 ]; then
      echo "Something went wrong in run $i, check the logs"
      exit 1
  fi
done