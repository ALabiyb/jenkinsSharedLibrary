import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def call(String repoUrl, String manifestPath = 'manifest.json', String branch = 'main') {
    // Clone the repo to a temp directory
    def tempDir = "${env.WORKSPACE}/tempRepo"
    sh "rm -rf ${tempDir}"
    sh "git clone --depth 1 --branch ${branch} ${repoUrl} ${tempDir}"

    // Read the manifest file
    def manifestFile = "${tempDir}/${manifestPath}"
    def manifest = new JsonSlurper().parse(new File(manifestFile))

    // Update manifest (example: set lastUpdated to current date)
    manifest.lastUpdated = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone('UTC'))

    // Write back the updated manifest
    new File(manifestFile).text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest))

    // Optionally, commit and push changes
    sh """
        cd ${tempDir}
        git config user.email "jenkins@example.com"
        git config user.name "Jenkins"
        git add ${manifestPath}
        git commit -m "Update manifest: lastUpdated set to ${manifest.lastUpdated}"
        git push origin ${branch}
    """

    // Clean up
    sh "rm -rf ${tempDir}"
}