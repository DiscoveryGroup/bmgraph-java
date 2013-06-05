#!/bin/sh

mvn install::install-file \
    -Dfile=target/bmgraph-1.0-SNAPSHOT.jar \
    -DgroupId=biomine.bmgraph \
    -DartifactId=bmgraph \
    -Dversion=1.0-SNAPSHOT \
    -Dpackaging=jar
