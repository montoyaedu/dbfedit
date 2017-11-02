#!/bin/bash
if [ -z "$1" ]; then
    echo "No argument supplied"
    echo "Usage:"
    echo "$0 <DBF FILE>"
    exit 1
fi
mvn exec:java -Dexec.mainClass="it.ethiclab.dbfedit.DbfEdit" -Dexec.args="$1"
