/**
 * buildAppandDb.groovy
 * A Jenkins shared library function to build application only.
 */

def call(Map params = [:]) {
    //Extract parameters with default values
    def projectName = params.projectName ?: env.JOB_NAME
    def imageName = params.imageName ?: env.JOB_NAME?.toLowerCase()?.replaceAll(/[^a-z0-9\-_.]/, '-')
    def imageTag = params.imageTag ?: env.BUILD_NUMBER ?: 'latest'
    def registryUrl = params.registryUrl ?: ''
    def registryCredentialsId = params.registryCredentialsId ?: ''
    def dockerfilePath = params.dockerfilePath ?: './Dockerfile'
    def buildContext = params.buildContext ?: '.'
    def buildArgs = params.buildArgs ?: [:]
    def pushToRegistry = params.pushToRegistry ?: true
    def removeAfterPush = params.removeAfterPush ?: true
    def gitAuthor = params.gitAuthor ?: 'unknown'
    def gitCommitMessage = params.gitCommitMessage ?: 'No commit message'



    echo "=== Building Application Docker Image ==="
    echo "Project Name: ${projectName}"
    echo "Image Name: ${imageName}"
    echo "Image Tag: ${imageTag}"
    echo "Registry URL: ${registryUrl}"
    echo "Push to Registry: ${pushToRegistry}"
    echo "Git Author: ${gitAuthor}"

//     try {
//         stage('Build Application Image') {
//             echo "🔨 Building application Docker image..."
//             // Build the application Docker image
//             def fullImageName = registryUrl ? "${registryUrl}/${imageName}:${imageTag}" : "${imageName}:${imageTag}"

//             // Validate Dockerfile exists
//             if (!fileExists(dockerfilePath)) {
//                 error "Dockerfile not found at path: ${dockerfilePath}"
//             }

//             // Build the Docker image
//             def buildImage = docker.build(fullImageName, "-f ${dockerfilePath} ${buildContext}")
// //            def buildResult = buildDockerImage(fullImageName, dockerfilePath, buildContext, buildArgs)

// //            if (!buildResult.success) {
// //                error "Failed to build Docker image: ${buildResult.error}"
// //            }

//             echo "✅ Successfully built Docker image: ${fullImageName}"

            /// Push to registry if requested
            // if (pushToRegistry && registryUrl) {
            //     if (registryCredentialsId) {
            //         docker.withRegistry("https://${registryUrl}", registryCredentialsId) {
            //             dockerImage.push()
            //             echo "✅ Successfully pushed to registry: ${registryUrl}"
            //         }
            //     } else {
            //         dockerImage.push()
            //         echo "✅ Successfully pushed to registry: ${registryUrl}"
            //     }

            //     // Remove local image after push if requested
            //     if (removeAfterPush) {
            //         sh "docker rmi ${fullImageName} || true"
            //         echo "🗑️ Removed local image: ${fullImageName}"
            //     }
            // }

            // Get image info for return
//            def imageInfo = getImageInfo(fullImageName)

    //         return [
    //             success: true,
    //             imageName: fullImageName,
    //             pushed: pushToRegistry
    //         ]
    //     }

    // } catch (Exception e) {
    //     echo "❌ Build failed: ${e.getMessage()}"
    //     return [
    //         success: false,
    //         error: e.getMessage(),
    //         imageName: null,
    //         imageId: null,
    //         imageSize: null,
    //         buildDuration: null,
    //         pushed: false
    //     ]
    // }
}