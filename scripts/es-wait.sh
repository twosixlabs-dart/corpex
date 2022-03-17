#!/bin/bash

i=1

while [[ $i -ne 0 ]] ;
do
    curl --request GET -sL \
         --url 'http://localhost:9200/_cluster/health'\
         --output /dev/null
    i=$?
done
