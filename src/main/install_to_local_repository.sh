#!/bin/sh

mvn install::install-file \
    -Dfile=target/bmgraph-SNAPSHOT.jar \
    -DgroupId=biomine.bmgraph \
    -DartifactId=bmgraph \
    -Dversion=SNAPSHOT \
    -Dpackaging=jar
