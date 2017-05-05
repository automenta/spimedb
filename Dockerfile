FROM automenta/narchy

RUN apt-get install -y npm ; ln -s /usr/bin/nodejs /usr/bin/node
RUN npm i -g bower

RUN cd / ; git clone --depth 1 https://github.com/automenta/spimedb.git spimedb

# build: webapp client (bower)
RUN cd /spimedb/src/main/resources/public ; bower i --allow-root ; bower prune --allow-root

# build: server (maven)
RUN cd /spimedb ; mvn install -U -Dmaven.test.skip=true

WORKDIR /spimedb



