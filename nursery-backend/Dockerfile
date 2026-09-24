FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests


FROM tomcat:10.1.59-jdk21-temurin-jammy

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/nursery.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 10000

CMD sed -i "s/port=\"8080\"/port=\"${PORT:-10000}\"/" /usr/local/tomcat/conf/server.xml && catalina.sh run