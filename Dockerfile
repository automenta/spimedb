FROM automenta/narchy

# https://docs.docker.com/engine/tutorials/dockervolumes/

# RW volume: docker run -d -P --name web -v /src/webapp:/webapp training/webapp python app.py
# RO volume: docker run -d -P --name web -v /src/webapp:/webapp:ro training/webapp python app.py

# https://github.com/docker/labs/blob/master/developer-tools/java/chapters/appa-common-commands.adoc
# https://docs.docker.com/engine/tutorials/networkingcontainers/
# https://github.com/docker/labs/blob/master/developer-tools/java/chapters/ch04-run-container.adoc
# https://github.com/docker/labs/blob/master/developer-tools/java/chapters/ch10-monitoring.adoc

RUN cd / ; git clone --depth 1 https://github.com/automenta/spimedb.git spimedb

# RUN rm ~/.m2/repository/narchy/util/1.0/_remote.repositories ~/.m2/repository/narchy/nal/1.0/_remote.repositories

# RUN cd /spimedb ; mvn --projects core,logic,media install -Dmaven.test.skip=true



