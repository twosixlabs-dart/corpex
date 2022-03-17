#!/bin/bash

cd /corpex || exit 1

#i=1
#
#while [[ $i -ne 0 ]] ;
#do
#    curl --request GET -sL \
#         --url 'http://cdr-es:9200/_cluster/health'\
#         --output /dev/null
#    i=$?
#done

export GITLAB_CI=true

sbt integration:test || exit 1
