# Setup Dependencies

## Java

Install JDK >= 11 Any openjdk compliant version e.g.

https://adoptopenjdk.net/?variant=openjdk11
https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html

## Maven

Maven >= 3.6

https://maven.apache.org/download.cgi

## Docker

To run the applications databases and the tests you need to have docker running on your development machine.

# Setup IDE

## Install lombok.jar

Eclipse: https://projectlombok.org/setup/eclipse
IntelliJ: https://projectlombok.org/setup/intellij

## Enable micronaut annotation processor

https://docs.micronaut.io/latest/guide/index.html#ideSetup

# Start debug session

Set admin-insecure-mode in your `.env` file (`ACAPY_ADMIN_CONFIG=--admin-insecure-mode`)

Run the following command in the repository root to run aca-py plus its dependencies (database):

`docker-compose  -f docker-compose-backend.yml up aca-py`

Start a debug session in your IDE with `-Dmicronaut.environments=dev`.

