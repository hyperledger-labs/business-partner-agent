@Library('bds-pipeline') _

pipeline {

   agent any

   environment {
        REG_IMAGE = 'agent-registry'
        SETTINGS_ID = 'a77b84ea-0de6-4ede-8961-060229a96fcf'
        MVN_VERSION = 'maven-3.6.x'
    }

   options { 
      buildDiscarder(logRotator(numToKeepStr: '10')) 
   }

   stages {
      stage('Maven Build') {
         steps {
            dir("backend") {
               withMaven(
                  jdk: "java-11",
                  maven: "${MVN_VERSION}",
                  globalMavenSettingsConfig: "${SETTINGS_ID}") {
                  sh "mvn clean deploy -Pbuild-frontend -DaltDeploymentRepository=bds-nexus::default::https://nexus.bosch-digital.com/repository/bds-snapshots/"
                  sh "mvn docker:build docker:push -Ddocker.push.registry=nexus.bosch-digital.com:5000 -Ddocker.name=bds/%a"
               }
            }
         }
      }
   }
}