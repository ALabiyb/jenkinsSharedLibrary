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
}