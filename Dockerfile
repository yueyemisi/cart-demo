FROM bellsoft/liberica-openjre-debian:17.0.3 as extracter

COPY target/*.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract

FROM bellsoft/liberica-openjre-debian:17.0.3

WORKDIR /home

COPY --from=extracter /dependencies/ ./
RUN true
COPY --from=extracter /spring-boot-loader ./
RUN true
COPY --from=extracter /snapshot-dependencies/ ./
RUN true
COPY --from=extracter /application/ ./
RUN true

ENV TZ=Asia/Shanghai

RUN apt-get install -y curl fontconfig libfreetype6 && \
         ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
         echo '$TZ' > /etc/timezone

ENTRYPOINT exec \
                            java \
                            ${JAVA_OPTS} \
                            -Dfile.encoding=UTF-8 \
                            -Duser.timezone=${TZ} \
                            -Djava.security.egd=file:/dev/./urandom \
                            org.springframework.boot.loader.JarLauncher \
                            ${APP_OPTS}