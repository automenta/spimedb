https://docs.docker.com/engine/tutorials/dockervolumes/

RW volume: docker run -d -P --name web -v /src/webapp:/webapp training/webapp python app.py
RO volume: docker run -d -P --name web -v /src/webapp:/webapp:ro training/webapp python app.py

https://github.com/docker/labs/blob/master/developer-tools/java/chapters/appa-common-commands.adoc
https://docs.docker.com/engine/tutorials/networkingcontainers/
https://github.com/docker/labs/blob/master/developer-tools/java/chapters/ch04-run-container.adoc
https://github.com/docker/labs/blob/master/developer-tools/java/chapters/ch10-monitoring.adoc

docker run -p 8080:8080 --rm  -it automenta/spimedb

java -jar /spimedb/media/target/spimedb-media-1.0.one-jar.jar


docker run -it -p 127.0.0.1:8080:8080  -v /EA:/data:ro jkilbride/node-npm-alpine sh
docker run -it -p 8080:8080  -v /EA:/data:ro jkilbride/node-npm-alpine sh
    npm i -g http-server ; http-server /data -g -s -r
        also node-ecstatic: https://github.com/jfhbrook/node-ecstatic