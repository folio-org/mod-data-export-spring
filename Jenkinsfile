buildMvn {
  publishModDescriptor = 'yes'
  mvnDeploy = 'yes'
  doKubeDeploy = true
  buildNode = 'jenkins-agent-java21'

  doDocker = {
    buildDocker {
      publishMaster = 'yes'
      // healthChk for /admin/health in InstallUpgradeIT.java
    }
  }
}
