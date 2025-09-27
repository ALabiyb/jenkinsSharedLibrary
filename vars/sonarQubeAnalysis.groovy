/**
 * SonarQube Analysis Function for Jenkins Shared Library
 * File: vars/sonarQubeAnalysis.groovy
 */

def call(Map params = [:]) {
    // Default parameters
    def defaultParams = [
        projectKey: 'default-project',
        projectName: 'Default Project',
        sonarServerName: 'SonarQubeServer',
        javaHome: '/usr/lib/jvm/java-21-openjdk-amd64',
        mavenGoals: 'clean compile sonar:sonar',
        additionalParams: '',
        skipTests: false,
        enableQualityGate: true,
        qualityGateTimeout: 1,
        qualityGateTimeoutUnit: 'HOURS',
        abortPipelineOnFailure: false
    ]
    
    // Merge user parameters with defaults
    def config = defaultParams + params
    
    try {
        echo "==== Starting SonarQube Analysis ===="
        echo "Project Key: ${config.projectKey}"
        echo "Project Name: ${config.projectName}"
        echo "SonarQube Server: ${config.sonarServerName}"
        
        // Get Maven tool
        def mavenHome = tool name: 'maven', type: 'maven'
        
        // Set environment variables
        withEnv([
            "JAVA_HOME=${config.javaHome}", 
            "PATH=${config.javaHome}/bin:${mavenHome}/bin:${env.PATH}"
        ]) {
            withSonarQubeEnv(config.sonarServerName) {
                sh """
                    echo "=== Verifying Tools ==="
                    echo "JAVA_HOME: \$JAVA_HOME"
                    which java
                    which javac
                    which mvn
                    java -version
                    javac -version
                    mvn -version
                    
                    echo "=== Starting SonarQube Analysis ==="
                    mvn ${config.mavenGoals} \\
                    -Dsonar.projectKey=${config.projectKey} \\
                    -Dsonar.projectName='${config.projectName}' \\
                    -Dmaven.compiler.fork=true \\
                    -Dmaven.compiler.executable=\${JAVA_HOME}/bin/javac \\
                    ${config.skipTests ? '-DskipTests=true' : ''} \\
                    ${config.additionalParams}
                """
            }
        }
        
        echo "✅ SonarQube analysis completed successfully"
        
        // Return success result
        return [
            success: true,
            message: "SonarQube analysis completed successfully",
            projectKey: config.projectKey,
            projectName: config.projectName
        ]
        
    } catch (Exception e) {
        echo "❌ SonarQube analysis failed: ${e.getMessage()}"
        
        if (config.abortPipelineOnFailure) {
            error("SonarQube analysis failed: ${e.getMessage()}")
        }
        
        // Return failure result
        return [
            success: false,
            error: e.getMessage(),
            message: "SonarQube analysis failed: ${e.getMessage()}",
            projectKey: config.projectKey,
            projectName: config.projectName
        ]
    }
}