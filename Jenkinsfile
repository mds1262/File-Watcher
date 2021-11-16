podTemplate(label: 'mediaWatcher2-BUILD',
  containers: [
    containerTemplate(
      name: 'git',
      image: 'alpine/git',
      command: 'cat',
      ttyEnabled: true
    ),
    containerTemplate(
      name: 'docker',
      image: 'docker',
      command: 'cat',
      ttyEnabled: true
    )
  ],
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
  ],
) {
    node('mediaWatcher2-BUILD') {
        def dockerHubCred = "dev-reg"
        def dockerRegistry = "http://dev-reg.kollus.com:30003"
        def appImage

        stage('Checkout'){
            container('git'){
                checkout scm
            } //container
        } //stage

      stage('DOCKER-BUILD'){
        container('docker'){
            stage('Docker Image Build'){
	            appImage = docker.build("kollus/mediawatcher2", "-f Dockerfile-Jenkins --no-cache=false .")
	        }//stage
	        stage('Docker Image Build'){
	            appImage.inside{
                    sh 'ls -al'
	            }//inside
	        }//stage
        }//container
      }//stage
      
      stage('DOCKER-PUSH'){
        container('docker'){
            script {
                docker.withRegistry(dockerRegistry, dockerHubCred){
                    appImage.push("${env.BUILD_NUMBER}")
                    appImage.push("latest")
                }//withRegistry
            }// script
         }//container
      }//stage
    
    }// node
}
