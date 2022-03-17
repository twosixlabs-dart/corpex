#!/bin/bash

docker-compose -f docker-compose-test.yml up -d

java -jar corpex-microservice/target/scala*/corpex*.jar --env default

docker-compose -f docker-compose-test.yml down
