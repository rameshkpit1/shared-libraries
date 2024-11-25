#!groovy
def call(Map configMap){
  def component = configMap.get("component")
  echo "component is: $component"
    pipeline{
      agent { node { label 'agent-1'}}
      environment{
        SONAR_SCANNER=tool 'sonar'
        packageVersion = ''
      }
      stages{
        stage("Retrieve version"){
          steps{
            script{
              def packageJson = readJSON(file: 'package.json')
              packageVersion = packageJson.version
              echo "this is the version: ${packageVersion}"
            }
          }
        }
        stage("install dependencies"){
          steps{
            sh 'npm install'
          }
        }
        stage('unit testing'){
          steps{
            echo "doing unit testing"
          }
        }
        stage('sonar analysis'){
          steps{
            withSonarQubeEnv('sonar'){
              sh  "$SONAR_SCANNER/bin/sonar-scanner -Dsonar.projectName=${component} -Dsonar.projectKey=${params.component}"
            }
          }
        }
        stage('Quality Check') {
          steps {
            script {
              waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token' 
            }
          }
        }
        stage('OWASP Dependency-Check Scan') {
          steps {
                    dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
          }
        }
        stage('Trivy scan file scan'){
          steps{
            sh 'trivy fs . > trivyscan.txt'
          }
      }
      stage('Build'){
        steps{
          sh 'ls -ltr'
          sh "zip -r ${component}.zip ./* --exclude=.git --exclude=.zip --exclude=trivyscan.txt --exclude=Jenkinsfile"
        }
      }
      stage('artifcate upload to nexus'){
        steps{
          nexusArtifactUploader(
            nexusVersion: 'nexus3',
            protocol: 'http',
            nexusUrl: '172.31.95.186:8081/',
            groupId: 'com.roboshop',
            version: "$packageVersion",
            repository: "${component}", //which we created in nexus
            credentialsId: 'nexus-auth',
            artifacts: [
                [artifactId: "${component}",
                  classifier: '',
                  file: "${component}.zip",
                  type: 'zip']
            ]
          )
        }
      }
      stage("Deploy in env"){
        steps{
          script{
            echo "deploying"
            echo "$packageVersion"
            def params = [
              string(name: 'version', value: "$packageVersion"),
              string(name: 'environment', value: "dev")
            ]
            build job: "../${component}-deploy", wait: true, parameters: params
          }
        }
      }
      }
      post {
        always{
          echo "cleaning up workspace dir"
          //deleteDir()

        }
      }  
    }
}
