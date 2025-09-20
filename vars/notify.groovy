def call (Map params = [:]) {
    // Required parameters with defaults
    def subject = params.get('subject', 'Jenkins Notification')
    def recipients = params.get('recipients', '')
    def templateName = params.get('templateName', 'start.html')
    def data = params.get('data', [:])

    echo "=== Sending Email Notification ==="
    echo "Subject: ${subject}"
    echo "Recipients: ${recipients}"
    echo "Template: ${templateName}"
    echo "Data keys: ${data.keySet()}"

    if (!recipients) {
        echo "⚠️  No recipients specified, skipping email notification"
        return
    }

    try {
        // Load the email template
        echo "Loading template: ${templateName}"
        def template = getTemplateByName(templateName)


        if (!template) {
            echo "❌ Template '${templateName}' is empty or not found"
            // Fallback to a simple default template
            template = getDefaultTemplate()
        }

        // Render the template with the provided data
        def renderedTemplate = renderTemplate(template, data)

        // Send the email notification
        emailext(
                subject: subject,
                body: renderedTemplate,
                to: recipients,
                mimeType: 'text/html',
                attachLog: false, // Set to true if you want to attach build logs
                recipientProviders: [
                        [$class: 'DevelopersRecipientProvider'],
                        [$class: 'RequesterRecipientProvider']
                ]
        )

        echo "✅ Email notification sent successfully to: ${recipients}"

    } catch (Exception e) {
        echo "❌ Failed to send email notification: ${e.getMessage()}"
        echo "Error details: ${e.toString()}"

        // Try to send a simple fallback notification
        try {
            sendFallbackNotification(subject, recipients, data)
        } catch (Exception fallbackError) {
            echo "❌ Fallback notification also failed: ${fallbackError.getMessage()}"
        }

        // Don't fail the build for notification errors
        echo "⚠️  Continuing pipeline execution despite notification failure"
    }

}

/**
 * Render template by replacing placeholders with actual data
 */
def renderTemplate(String template, Map data) {
    def renderedTemplate = template

    // Replace all placeholders in the format {{KEY}}
    data.each { key, value ->
        def placeholder = "{{${key}}}"
        def replacement = value?.toString() ?: "N/A"
        renderedTemplate = renderedTemplate.replace(placeholder, replacement)
        echo "Replaced ${placeholder} with: ${replacement}"
    }

    // Replace any remaining unreplaced placeholders with "N/A"
    renderedTemplate = renderedTemplate.replaceAll(/\{\{[^}]+\}\}/, "N/A")

    return renderedTemplate
}


/**
 * Get template by name (inline templates)
 */
def getTemplateByName(String templateName) {
    def templates = [
            'start.html': getStartTemplate(),
            'success.html': getSuccessTemplate(),
            'failure.html': getFailureTemplate(),
            'fail.html': getFailureTemplate()  // Alias for failure
    ]

    return templates.get(templateName, null)
}

/**
 * Start template
 */
def getStartTemplate() {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { 
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                margin: 0; 
                padding: 20px;
                background-color: #f5f5f5;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background-color: white;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                overflow: hidden;
            }
            .header { 
                background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
                color: white;
                padding: 20px;
                text-align: center;
            }
            .content { 
                padding: 30px;
                line-height: 1.6;
            }
            .info-table {
                width: 100%;
                border-collapse: collapse;
                margin: 20px 0;
            }
            .info-table td {
                padding: 12px 15px;
                border-bottom: 1px solid #e0e0e0;
            }
            .info-table td:first-child {
                font-weight: bold;
                background-color: #f0f0f0;
                width: 150px;
            }
            .button {
                display: inline-block;
                background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
                color: white;
                padding: 12px 25px;
                text-decoration: none;
                border-radius: 5px;
                font-weight: bold;
                margin: 20px 0;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>🚀 Pipeline Started</h1>
                <p>Jenkins Build Notification</p>
            </div>
            
            <div class="content">
                <p>Hello!</p>
                <p>A new Jenkins pipeline has been started. Here are the details:</p>
                
                <table class="info-table">
                    <tr><td>Job Name</td><td>{{JOB_NAME}}</td></tr>
                    <tr><td>Build Number</td><td>#{{BUILD_NUMBER}}</td></tr>
                    <tr><td>Branch</td><td>{{BRANCH}}</td></tr>
                    <tr><td>Triggered By</td><td>{{TRIGGERED_BY}}</td></tr>
                    <tr><td>Commit</td><td>{{GIT_COMMIT}}</td></tr>
                </table>
                
                <a href="{{BUILD_URL}}" class="button">View Build Details</a>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Success template
 */
def getSuccessTemplate() {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }
            .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 20px; text-align: center; }
            .content { padding: 30px; }
            .success { color: #28a745; font-weight: bold; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>✅ Pipeline Successful</h1>
            </div>
            <div class="content">
                <p>Great news! The pipeline completed <span class="success">successfully</span>.</p>
                <p><strong>Job:</strong> {{JOB_NAME}} #{{BUILD_NUMBER}}</p>
                <p><strong>Duration:</strong> {{BUILD_DURATION}}</p>
                <p><strong>Branch:</strong> {{BRANCH}}</p>
                <p><a href="{{BUILD_URL}}">View Build Details</a></p>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Failure template
 */
def getFailureTemplate() {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }
            .header { background: linear-gradient(135deg, #dc3545, #c82333); color: white; padding: 20px; text-align: center; }
            .content { padding: 30px; }
            .failure { color: #dc3545; font-weight: bold; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>❌ Pipeline Failed</h1>
            </div>
            <div class="content">
                <p>The pipeline has <span class="failure">failed</span>. Please check the details below:</p>
                <p><strong>Job:</strong> {{JOB_NAME}} #{{BUILD_NUMBER}}</p>
                <p><strong>Branch:</strong> {{BRANCH}}</p>
                <p><strong>Failure Reason:</strong> {{FAILURE_REASON}}</p>
                <p><a href="{{BUILD_URL}}">View Build Logs</a></p>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Send a simple fallback notification if the main template fails
 */
def sendFallbackNotification(String subject, String recipients, Map data) {
    echo "Attempting fallback notification..."

    def simpleBody = """
    Jenkins Pipeline Notification
    
    Job: ${data.JOB_NAME ?: 'Unknown'}
    Build: #${data.BUILD_NUMBER ?: 'Unknown'}
    Branch: ${data.BRANCH ?: 'Unknown'}
    Status: ${data.BUILD_STATUS ?: 'Unknown'}
    Triggered by: ${data.TRIGGERED_BY ?: 'Unknown'}
    
    Build URL: ${data.BUILD_URL ?: 'Unknown'}
    
    This is a fallback notification sent because the HTML template failed to load.
    """

    emailext(
            subject: "[FALLBACK] ${subject}",
            body: simpleBody,
            to: recipients,
            mimeType: 'text/plain'
    )

    echo "✅ Fallback notification sent"
}