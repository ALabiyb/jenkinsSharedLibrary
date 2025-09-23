/**
 * pushToRepository.groovy
 * A Jenkins shared library function to push Image to repo.
 */
def call(Map params = [:]) {
    // Required parameters: imageName, imageTag, registryType (company or dockerhub), credentialsId
    def imageName = params.get('imageName')
    def imageTag = params.get('imageTag', 'latest')
    def registryType = params.get('registryType', 'dockerhub')
    def credentialsId = params.get('credentialsId')
    // def companyRegistry = params.get('companyRegistry', 'registry.company.com')
    def privateRegistryUrl = params.get('privateRegistryUrl', 'registry.company.com')
    def removeAfterPush = params.get('removeAfterPush', true)
    def failOnError = params.containsKey('failOnError') ? params.failOnError : true  // default: fail pipeline on error
    
    try {
        if (!imageName || !credentialsId) {
            error "imageName and credentialsId are required parameters"
        }

        // def fullImageName = ''
        def localImageName = "${imageName}:${imageTag}"
        def targetImageName = ''
        def registryUrl = ''

        switch(registryType.toLowerCase()) {
            case 'dockerhub':
                targetImageName = localImageName
                registryUrl = ''
                echo "Pushing to Docker Hub: ${targetImageName}"
                break
            case 'private':
                targetImageName = "${privateRegistryUrl}/${imageName}:${imageTag}"
                registryUrl = privateRegistryUrl
                echo "Pushing to Private Registry: ${targetImageName}"
            break
            default:
                error "Unsupported registryType: ${registryType}. Supported types are 'dockerhub' and 'private'."
        }

        
        // if (registryType == 'company') {
        //     fullImageName = "${companyRegistry}/${imageName}:${imageTag}"
        // } else {
        //     // dockerhub
        //     fullImageName = "${imageName}:${imageTag}"
        // }

        withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASS')]) {
            // if (registryType == 'company') {
            //     sh "echo \$REGISTRY_PASS | docker login ${companyRegistry} -u \$REGISTRY_USER --password-stdin"
            // } else {
            //     sh "echo \$REGISTRY_PASS | docker login -u \$REGISTRY_USER --password-stdin"
            // }

            if (registryType == 'private') {
                sh "echo \$REGISTRY_PASS | docker login ${companyRegistry} -u \$REGISTRY_USER --password-stdin"
            } else {
                sh "echo \$REGISTRY_PASS | docker login -u \$REGISTRY_USER --password-stdin"
            }
            // sh "docker tag ${imageName}:${imageTag} ${fullImageName}"
            if (targetImageName != localImageName) {
                echo "🏷️ Tagging image: ${localImageName} -> ${targetImageName}"
                sh "docker tag ${localImageName} ${targetImageName}"
            }
            sh "docker push ${targetImageName}"

            if (removeAfterPush) {                                    // ⭐ NEW: Conditional cleanup
                echo "🧹 Removing local images..."
                sh "docker rmi ${targetImageName} || true"
                if (targetImageName != localImageName) {             // ⭐ NEW: Smart cleanup
                    sh "docker rmi ${localImageName} || true"
                }
            }

            echo "🔓 Logging out from registry"
            sh "docker logout"
        }
        // env.BUILD_RESULT_PUSH_SUCCESS = 'true'

        // ⭐ NEW: Set environment variables
        env.BUILD_RESULT_PUSH_SUCCESS = 'true'
        env.FINAL_IMAGE_NAME = targetImageName

        // ⭐ NEW: Return detailed result
        return [
            success: true,
            imageName: targetImageName,
            registryType: registryType,
            pushed: true
        ]
    } catch (Exception e) {
        env.BUILD_RESULT_PUSH_SUCCESS = 'false'
        env.BUILD_RESULT_ERROR_TYPE = 'PUSH_TO_REGISTRY_ERROR'
        env.BUILD_RESULT_ERROR_MESSAGE = e.getMessage()
        env.BUILD_RESULT_MESSAGE = "Failed to push image to registry: ${e.getMessage()}"
        throw e
    }
}
