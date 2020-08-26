#!/usr/bin/env bash

for i in {1..10}
do
  echo "Running iteration $i"
  ./gradlew integrationtest --info
  if [ $? != 0 ]; then
      echo "Something went wrong in run $i, check the logs"
      exit 1
  fi
done