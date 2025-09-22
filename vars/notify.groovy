def call(Map params = [:]) {
    // Required parameters with defaults
    def subject = params.get('subject', 'Jenkins Notification')
    def recipients = params.get('recipients', '')
    def templateName = params.get('template', 'start.html')
    def data = params.get('data', [:])

    // Determine status from subject or data for color coding
    def status = determineStatus(subject, data)

    echo "=== Sending Email Notification ==="
    echo "Subject: ${subject}"
    echo "Recipients: ${recipients}"
    echo "Template: ${templateName}"
    echo "Status: ${status}"
    echo "Data keys: ${data.keySet()}"

    if (!recipients) {
        echo "⚠️  No recipients specified, skipping email notification"
        return
    }

    try {
        // Load the email template based on status
        echo "Loading template: ${templateName} for status: ${status}"
        def template = getTemplateByName(templateName, status)

        if (!template) {
            echo "❌ Template '${templateName}' is empty or not found"
            template = getDefaultTemplate(status)
        }

        // Add status-specific data
        data.put('STATUS_COLOR', getStatusColor(status))
        data.put('STATUS_ICON', getStatusIcon(status))
        data.put('STATUS_TEXT', getStatusText(status))

        // Add build details if available
        if (data.BUILD_RESULT) {
            def buildResult = data.BUILD_RESULT
            data.put('BUILD_SUCCESS', buildResult.buildSuccess ?: false)
            data.put('PUSH_SUCCESS', buildResult.pushSuccess ?: false)
            data.put('ERROR_TYPE', buildResult.errorType ?: '')
            data.put('DETAILED_MESSAGE', buildResult.message ?: '')
        }

        // Render the template with the provided data
        def renderedTemplate = renderTemplate(template, data)

        // Send the email notification
        emailext(
                subject: subject,
                body: renderedTemplate,
                to: recipients,
                mimeType: 'text/html',
                attachLog: status == 'FAILURE', // Attach logs only for failures
                recipientProviders: [
                        [$class: 'DevelopersRecipientProvider'],
                        [$class: 'RequesterRecipientProvider']
                ]
        )

        echo "✅ Email notification sent successfully to: ${recipients}"

    } catch (Exception e) {
        echo "❌ Failed to send email notification: ${e.getMessage()}"

        try {
            sendFallbackNotification(subject, recipients, data, status)
        } catch (Exception fallbackError) {
            echo "❌ Fallback notification also failed: ${fallbackError.getMessage()}"
        }

        echo "⚠️  Continuing pipeline execution despite notification failure"
    }
}

/**
 * Determine status from subject or data
 */
def determineStatus(String subject, Map data) {
    def subjectLower = subject.toLowerCase()
    def buildStatus = data.BUILD_STATUS?.toString()?.toUpperCase()

    if (subjectLower.contains('failed') || subjectLower.contains('❌') || buildStatus == 'FAILED') {
        return 'FAILURE'
    } else if (subjectLower.contains('success') || subjectLower.contains('✅') || buildStatus == 'SUCCESS') {
        return 'SUCCESS'
    } else if (subjectLower.contains('started') || subjectLower.contains('🚀') || buildStatus == 'STARTED') {
        return 'STARTED'
    } else if (subjectLower.contains('warning') || subjectLower.contains('⚠️')) {
        return 'WARNING'
    }
    return 'INFO'
}

/**
 * Get status color
 */
def getStatusColor(String status) {
    switch (status) {
        case 'SUCCESS': return '#28a745'
        case 'FAILURE': return '#dc3545'
        case 'STARTED': return '#007bff'
        case 'WARNING': return '#ffc107'
        default: return '#6c757d'
    }
}

/**
 * Get status icon
 */
def getStatusIcon(String status) {
    switch (status) {
        case 'SUCCESS': return '✅'
        case 'FAILURE': return '❌'
        case 'STARTED': return '🚀'
        case 'WARNING': return '⚠️'
        default: return 'ℹ️'
    }
}

/**
 * Get status text
 */
def getStatusText(String status) {
    switch (status) {
        case 'SUCCESS': return 'Success'
        case 'FAILURE': return 'Failed'
        case 'STARTED': return 'Started'
        case 'WARNING': return 'Warning'
        default: return 'Info'
    }
}

/**
 * Render template by replacing placeholders with actual data
 */
def renderTemplate(String template, Map data) {
    def renderedTemplate = template

    data.each { key, value ->
        def placeholder = "{{${key}}}"
        def replacement = value?.toString() ?: "N/A"
        renderedTemplate = renderedTemplate.replace(placeholder, replacement)
        echo "Replaced ${placeholder} with: ${replacement}"
    }

    renderedTemplate = renderedTemplate.replaceAll(/\{\{[^}]+\}\}/, "N/A")
    return renderedTemplate
}

/**
 * Get template by name with status-aware styling
 */
def getTemplateByName(String templateName, String status) {
    def templates = [
            'start.html': getStartTemplate(status),
            'success.html': getSuccessTemplate(status),
            'failure.html': getFailureTemplate(status),
            'warning.html': getWarningTemplate(status),
            'fail.html': getFailureTemplate(status)
    ]

    return templates.get(templateName, null)
}

/**
 * Enhanced Start template with dynamic colors
 */
def getStartTemplate(String status) {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 0;
                padding: 20px;
                background-color: #f8f9fa;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background-color: white;
                border-radius: 10px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                overflow: hidden;
                border: 3px solid #007bff;
            }
            .header {
                background: linear-gradient(135deg, #007bff, rgba(#007bff, 0.8));
                color: white;
                padding: 30px 20px;
                text-align: center;
            }
            .status-badge {
                background: rgba(255,255,255,0.2);
                padding: 8px 16px;
                border-radius: 20px;
                display: inline-block;
                margin-bottom: 10px;
                font-weight: bold;
            }
            .content {
                padding: 30px;
                line-height: 1.6;
            }
            .info-grid {
                display: grid;
                grid-template-columns: 150px 1fr;
                gap: 15px;
                margin: 20px 0;
            }
            .info-label {
                font-weight: bold;
                color: #495057;
                background-color: #e9ecef;
                padding: 10px;
                border-radius: 5px;
            }
            .info-value {
                padding: 10px;
                background-color: #f8f9fa;
                border-radius: 5px;
                border-left: 4px solid #007bff;
            }
            .button {
                display: inline-block;
                background: linear-gradient(135deg, #007bff, rgba(#28a745, 0.8));
                color: white;
                padding: 12px 25px;
                text-decoration: none;
                border-radius: 25px;
                font-weight: bold;
                margin: 20px 0;
                transition: transform 0.2s;
            }
            .button:hover {
                transform: translateY(-2px);
            }
            .footer {
                background-color: #f8f9fa;
                padding: 20px;
                text-align: center;
                color: #6c757d;
                font-size: 12px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <div class="status-badge">{{STATUS_ICON}} {{STATUS_TEXT}}</div>
                <h1>Pipeline {{STATUS_TEXT}}</h1>
                <p>Jenkins Build Notification</p>
            </div>

            <div class="content">
                <p>Hello!</p>
                <p>A Jenkins pipeline has been {{STATUS_TEXT}}. Here are the details:</p>

                <div class="info-grid">
                    <div class="info-label">Job Name</div>
                    <div class="info-value">{{JOB_NAME}}</div>

                    <div class="info-label">Build Number</div>
                    <div class="info-value">#{{BUILD_NUMBER}}</div>

                    <div class="info-label">Branch</div>
                    <div class="info-value">{{BRANCH}}</div>

                    <div class="info-label">Triggered By</div>
                    <div class="info-value">{{TRIGGERED_BY}}</div>

                    <div class="info-label">Git Author</div>
                    <div class="info-value">{{GIT_AUTHOR}}</div>

                    <div class="info-label">Commit Message</div>
                    <div class="info-value">{{GIT_COMMIT}}</div>
                </div>

                <a href="{{BUILD_URL}}" class="button">{{STATUS_ICON}} View Build Details</a>
            </div>

            <div class="footer">
                <p>Automated notification from Jenkins CI/CD Pipeline</p>
                <p>{{CHANGE_TYPES}} changes detected</p>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Enhanced Success template
 */
def getSuccessTemplate(String status) {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f8f9fa; }
            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); overflow: hidden; border: 3px solid #28a745; }
            .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 30px 20px; text-align: center; }
            .content { padding: 30px; line-height: 1.6; }
            .success-badge { background: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 20px; display: inline-block; margin-bottom: 10px; font-weight: bold; }
            .build-details { background: #d4edda; border: 1px solid #c3e6cb; border-radius: 8px; padding: 20px; margin: 20px 0; }
            .metric { display: inline-block; margin: 10px 15px; text-align: center; }
            .metric-value { font-size: 24px; font-weight: bold; color: #28a745; }
            .metric-label { font-size: 12px; color: #6c757d; }
            .button { display: inline-block; background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 12px 25px; text-decoration: none; border-radius: 25px; font-weight: bold; margin: 20px 0; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <div class="success-badge">✅ Success</div>
                <h1>Pipeline Completed Successfully!</h1>
                <p>🎉 Build #{{BUILD_NUMBER}} finished without errors</p>
            </div>

            <div class="content">
                <p>Great news! The pipeline completed successfully.</p>

                <div class="build-details">
                    <h3>Build Summary</h3>
                    <p><strong>Job:</strong> {{JOB_NAME}} #{{BUILD_NUMBER}}</p>
                    <p><strong>Branch:</strong> {{BRANCH}}</p>
                    <p><strong>Image:</strong> {{IMAGE_NAME}}</p>
                    <p><strong>Build Success:</strong> {{BUILD_SUCCESS}}</p>
                    <p><strong>Push Success:</strong> {{PUSH_SUCCESS}}</p>
                    <p><strong>Message:</strong> {{DETAILED_MESSAGE}}</p>
                </div>

                <div style="text-align: center;">
                    <div class="metric">
                        <div class="metric-value">{{BUILD_NUMBER}}</div>
                        <div class="metric-label">BUILD NUMBER</div>
                    </div>
                    <div class="metric">
                        <div class="metric-value">✅</div>
                        <div class="metric-label">STATUS</div>
                    </div>
                </div>

                <a href="{{BUILD_URL}}" class="button">View Build Details</a>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Enhanced Failure template with detailed error information
 */
def getFailureTemplate(String status) {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f8f9fa; }
            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); overflow: hidden; border: 3px solid #dc3545; }
            .header { background: linear-gradient(135deg, #dc3545, #c82333); color: white; padding: 30px 20px; text-align: center; }
            .content { padding: 30px; line-height: 1.6; }
            .failure-badge { background: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 20px; display: inline-block; margin-bottom: 10px; font-weight: bold; }
            .error-details { background: #f8d7da; border: 1px solid #f5c6cb; border-radius: 8px; padding: 20px; margin: 20px 0; }
            .error-type { font-weight: bold; color: #721c24; margin-bottom: 10px; }
            .error-message { color: #721c24; font-family: monospace; background: rgba(114, 28, 36, 0.1); padding: 10px; border-radius: 4px; }
            .suggestions { background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 15px; margin: 20px 0; }
            .button { display: inline-block; background: linear-gradient(135deg, #dc3545, #c82333); color: white; padding: 12px 25px; text-decoration: none; border-radius: 25px; font-weight: bold; margin: 20px 0; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <div class="failure-badge">❌ Failed</div>
                <h1>Pipeline Failed</h1>
                <p>Build #{{BUILD_NUMBER}} encountered an error</p>
            </div>

            <div class="content">
                <p>The pipeline has failed. Please review the details below:</p>

                <div class="error-details">
                    <h3>Failure Details</h3>
                    <p><strong>Job:</strong> {{JOB_NAME}} #{{BUILD_NUMBER}}</p>
                    <p><strong>Branch:</strong> {{BRANCH}}</p>
                    <p><strong>Build Success:</strong> {{BUILD_SUCCESS}}</p>
                    <p><strong>Push Success:</strong> {{PUSH_SUCCESS}}</p>

                    <div class="error-type">Error Type: {{ERROR_TYPE}}</div>
                    <div class="error-message">{{ERROR_MESSAGE}}</div>

                    <p><strong>Detailed Message:</strong> {{DETAILED_MESSAGE}}</p>
                </div>

                <div class="suggestions">
                    <h4>💡 Suggested Actions</h4>
                    <ul>
                        <li>Check the build logs for detailed error information</li>
                        <li>If this is a certificate error, consider using allowInsecureRegistry: true</li>
                        <li>Verify your registry credentials are correct and not expired</li>
                        <li>Ensure your registry is accessible from the Jenkins agent</li>
                        <li>Contact your DevOps team if the issue persists</li>
                    </ul>
                </div>

                <a href="{{BUILD_URL}}" class="button">🔍 View Build Logs</a>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Warning template for partial failures
 */
def getWarningTemplate(String status) {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f8f9fa; }
            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); overflow: hidden; border: 3px solid #ffc107; }
            .header { background: linear-gradient(135deg, #ffc107, #e0a800); color: #212529; padding: 30px 20px; text-align: center; }
            .content { padding: 30px; line-height: 1.6; }
            .warning-badge { background: rgba(33, 37, 41, 0.1); padding: 8px 16px; border-radius: 20px; display: inline-block; margin-bottom: 10px; font-weight: bold; }
            .warning-details { background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 20px; margin: 20px 0; }
            .button { display: inline-block; background: linear-gradient(135deg, #ffc107, #e0a800); color: #212529; padding: 12px 25px; text-decoration: none; border-radius: 25px; font-weight: bold; margin: 20px 0; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <div class="warning-badge">⚠️ Warning</div>
                <h1>Pipeline Completed with Warnings</h1>
                <p>Build succeeded but with issues</p>
            </div>

            <div class="content">
                <p>The pipeline completed successfully but encountered some issues:</p>

                <div class="warning-details">
                    <h3>Warning Details</h3>
                    <p><strong>Job:</strong> {{JOB_NAME}} #{{BUILD_NUMBER}}</p>
                    <p><strong>Branch:</strong> {{BRANCH}}</p>
                    <p><strong>Build Success:</strong> {{BUILD_SUCCESS}}</p>
                    <p><strong>Push Success:</strong> {{PUSH_SUCCESS}}</p>
                    <p><strong>Warning Type:</strong> {{ERROR_TYPE}}</p>
                    <p><strong>Message:</strong> {{DETAILED_MESSAGE}}</p>
                </div>

                <a href="{{BUILD_URL}}" class="button">Review Details</a>
            </div>
        </div>
    </body>
    </html>
    """
}

/**
 * Default template fallback
 */
def getDefaultTemplate(String status) {
    return getStartTemplate(status)
}

/**
 * Send a simple fallback notification if the main template fails
 */
def sendFallbackNotification(String subject, String recipients, Map data, String status) {
    echo "Attempting fallback notification for status: ${status}"

    def statusIcon = getStatusIcon(status)
    def statusColor = getStatusColor(status)

    def simpleBody = """
    Jenkins Pipeline Notification ${statusIcon}

    Status: ${status}
    Job: ${data.JOB_NAME ?: 'Unknown'}
    Build: #${data.BUILD_NUMBER ?: 'Unknown'}
    Branch: ${data.BRANCH ?: 'Unknown'}
    Triggered by: ${data.TRIGGERED_BY ?: 'Unknown'}

    Build Details:
    - Build Success: ${data.BUILD_SUCCESS ?: 'Unknown'}
    - Push Success: ${data.PUSH_SUCCESS ?: 'Unknown'}
    - Error Type: ${data.ERROR_TYPE ?: 'None'}
    - Message: ${data.DETAILED_MESSAGE ?: data.ERROR_MESSAGE ?: 'No details available'}

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