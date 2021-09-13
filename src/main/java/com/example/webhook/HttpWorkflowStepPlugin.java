package com.example.webhook;

import static com.example.webhook.Utils.restTemplate;

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Main implementation of the plugin. This will handle fetching
 * tokens when they're expired and sending the appropriate request.
 */
@Plugin(name = HttpWorkflowStepPlugin.SERVICE_PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Rest HTTP Plugin", description = "Performs a GET, POST, PUT, DELETE to a rest resource")
public class HttpWorkflowStepPlugin implements StepPlugin {

    public static final String SERVICE_PROVIDER_NAME = "com.example.webhook.http.HttpWorkflowStepPlugin";

    @PluginProperty(title = "Remote URL", description = "Enter URL to be invoked.", required = true)
    String remoteUrl;

    @PluginProperty(title = "Methods HTTP", description = "Must be: \"GET\", \"POST\", \"PUT\", \"PATCH\", " +
        "\"DELETE\", " +
        "\"HEAD\", \"OPTIONS\"", required = true)
    @SelectValues(values = {"GET", "POST", "DELETE", "PUT", "PATCH", "HEAD", "OPTIONS"})
    String method;

    @PluginProperty(title = "Timeout", description = "Maximum wait time to respond, value in milliseconds.")
    int timeout;

    @PluginProperty(title = "Headers", description = "Headers for request")
    String headers;

    @PluginProperty(title = "Body", description = "Body for request")
    String body;

    @Override
    public void executeStep(PluginStepContext pluginStepContext, Map<String, Object> options) throws StepException {
        PluginLogger log = pluginStepContext.getLogger();

        if (remoteUrl == null || method == null) {
            throw new StepException("Remote URL and Method are required.", StepFailureReason.ConfigurationFailure);
        }

        if (timeout == 0) {
            log.log(1, "TimeOut not entered, default: 30000 mills");
            timeout = 30000;
        }

        if (remoteUrl.contains("${")) {
            remoteUrl = DataContextUtils.replaceDataReferencesInString(remoteUrl, pluginStepContext.getDataContext());
        }

        if (body != null && body.contains("${")) {
            body = DataContextUtils.replaceDataReferencesInString(body, pluginStepContext.getDataContext());
        }

        RestTemplate restTemplate;

        try {
            restTemplate = restTemplate(new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout)));
        } catch (GeneralSecurityException e) {
            log.log(0, e.getMessage());
            restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
        }

        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity<String> entity = null;

        requestHeaders.setExpires(0);
        requestHeaders.setCacheControl("no-cache, no-store, max-age=0, must-revalidate");

        if (headers != null) {
            requestHeaders = Utils.verifyHeadersAndSet(headers, log);
        }

        if (body != null) {
            entity = new HttpEntity<>(body);
            if (requestHeaders != null) {
                entity = new HttpEntity<>(body, requestHeaders);
            }
        }

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(remoteUrl,
                Objects.requireNonNull(HttpMethod.resolve(method)),
                entity, String.class);
            log.log(1, "######### PARAMETERS INPUT ##################");
            log.log(2, "URL: " + remoteUrl);
            log.log(2, "Method HTTP: " + method);
            if (requestHeaders != null) {
                log.log(2, "Headers:");
                for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
                    log.log(2, "Key: " + entry.getKey() + ", Value: " + entry.getValue());
                }
            }
            if (body != null) {
                log.log(2, "Body if Apply" + body);
            }
            log.log(1, "######### VALUES OUTPUT ##################");
            log.log(2, "Response Body: " + responseEntity.getBody());
            log.log(2, "Response Headers: " + responseEntity.getHeaders());
            log.log(2, "Status Code" + responseEntity.getStatusCodeValue());
        } catch (Exception e) {
            throw new StepException(e.getCause(), StepFailureReason.ConfigurationFailure);
        }
    }
}
