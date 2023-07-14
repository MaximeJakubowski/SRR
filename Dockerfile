FROM ubuntu:latest
COPY target/SRR-1.1-SNAPSHOT-jar-with-dependencies.jar /srrapp/SRR.jar

RUN apt-get update
RUN apt-get install -y --no-install-recommends openjdk-19-jre
RUN apt-get install -y software-properties-common
RUN apt-add-repository ppa:swi-prolog/stable
RUN apt-get update
RUN apt-get install -y swi-prolog swi-prolog-java

RUN SWI_HOME_DIR=/usr/lib/swi-prolog/
RUN LD_LIBRARY_PATH=/usr/lib/swi-prolog/lib/x86_64-linux/
RUN CLASSPATH=/usr/lib/swi-prolog/lib/jpl.jar
RUN LD_PRELOAD=/usr/lib/swi-prolog/lib/x86_64-linux/libswipl.so

ENTRYPOINT ["java", "-Djava.library.path=/usr/lib/swi-prolog/lib/x86_64-linux/",  "-jar", "/srrapp/SRR.jar"]

