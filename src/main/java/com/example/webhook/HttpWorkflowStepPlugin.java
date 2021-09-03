package com.example.webhook;

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import lombok.SneakyThrows;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Main implementation of the plugin. This will handle fetching
 * tokens when they're expired and sending the appropriate request.
 */
@Plugin(name = HttpWorkflowStepPlugin.SERVICE_PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Rest GET, POST, PUT, DELETE Plugin", description = "Performs a GET, POST, PUT, DELETE to a rest resource")
public class HttpWorkflowStepPlugin implements StepPlugin {

    public static final String SERVICE_PROVIDER_NAME = "com.example.webhook.http.HttpWorkflowStepPlugin";

    @SneakyThrows
    @Override
    public void executeStep(PluginStepContext pluginStepContext, Map<String, Object> options) throws StepException {
        PluginLogger log = pluginStepContext.getLogger();

        // Parse out the options
        String remoteUrl = options.containsKey("remoteUrl") ? options.get("remoteUrl").toString() : null;
        String method = options.containsKey("method") ? options.get("method").toString() : null;
        int timeout = options.containsKey("timeout") ? Integer.parseInt(options.get("timeout").toString()) : 30000;
        String headers = options.containsKey("headers") ? options.get("headers").toString() : null;
        String body = options.containsKey("body") ? options.get("body").toString() : null;

        if (remoteUrl == null || method == null) {
            throw new StepException("Remote URL and Method are required.", StepFailureReason.ConfigurationFailure);
        }

        if (remoteUrl.contains("${")) {
            remoteUrl = DataContextUtils.replaceDataReferencesInString(remoteUrl, pluginStepContext.getDataContext());
        }

        if (body != null && body.contains("${")) {
            body = DataContextUtils.replaceDataReferencesInString(body, pluginStepContext.getDataContext());
        }

        RestTemplate restTemplate = this.restTemplate(new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout)));

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
            restTemplate.exchange(remoteUrl, Objects.requireNonNull(HttpMethod.resolve(method)), entity, String.class);
        } catch (Exception e) {
            throw new StepException(e.getCause(), StepFailureReason.ConfigurationFailure);
        }
    }

    public RestTemplate restTemplate(RestTemplateBuilder builder) throws GeneralSecurityException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        return builder.requestFactory(() -> requestFactory).build();
    }
}
