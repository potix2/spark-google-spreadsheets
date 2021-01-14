@Library('vinted-shared-libs@master') _

env.REPOS_SOURCE = 'https://github.com/vinted/spark-google-spreadsheets'

if (env.ghprbSourceBranch) {
  ansiColor('xterm') {
    timestamps {
      node('slave') {
        withCredentials([usernamePassword(credentialsId: 'nexus_oom_user', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
          stage('Checkout') {
            build_cmd.handle_checkout()
          }
          stage('Test') {
            sh "echo $NEXUS_USER $NEXUS_PASSWORD"
            sh "./sbt/sbt compile"
          }
        }
      }
    }
  }
}
else {
  ansiColor('xterm') {
    timestamps {
      node('slave') {
        withCredentials([usernamePassword(credentialsId: 'nexus_oom_user', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
          stage('Checkout') {
            build_cmd.handle_checkout()
          }
          stage('Build') {
            sh "echo 'hello'"
          }
        }
      }
    }
  }
}
