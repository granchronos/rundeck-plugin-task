package com.example.webhook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class HttpWorkflowStepPluginTest {
    protected static final String REMOTE_URL = "/trigger";
    protected static final String BOGUS_URL = "/bogus";
    protected static final String REMOTE_SLOW_URL = "/slow-trigger";
    protected static final String ERROR_URL_500 = "/error500";
    protected static final String NO_CONTENT_URL = "/nocontent204";

    protected static final String BASE_URI_VALID = "https://dab6ea17-9691-4564-97a8-22352f3d7291.mock.pstmn.io/getTest1";

    protected static final int REQUEST_TIMEOUT = 2 * 1000;
    protected static final int SLOW_TIMEOUT = 3 * 1000;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);
    protected HttpWorkflowStepPlugin plugin;
    protected Map<String, Map<String, String>> dataContext;
    protected PluginStepContext pluginContext;
    protected PluginLogger pluginLogger;

    /**
     * Setup options for simple execution for the given method.
     *
     * @param method HTTP Method to use.
     * @return Options for the execution.
     */
    public Map<String, Object> getExecutionOptions(String method) {
        Map<String, Object> options = new HashMap<>();

        options.put("remoteUrl", Utils.BASE_URI + REMOTE_URL);
        options.put("method", method);

        return options;
    }

    /**
     * Setup options for execution for the given method using HTTP BASIC.
     *
     * @param method HTTP Method to use.
     * @return Options for the execution.
     */
    public Map<String, Object> getBasicOptions(String method) {
        return getExecutionOptions(method);
    }

    @Before
    public void setUp() {
        plugin = new HttpWorkflowStepPlugin();

        // Test all endpoints by simply iterating.
        for (String method : Utils.HTTP_METHODS) {
            // Simple endpoint
            WireMock.stubFor(WireMock.request(method, WireMock.urlEqualTo(REMOTE_URL)).atPriority(100)
                .willReturn(WireMock.aResponse()
                    .withStatus(200)));

            // 500 Error
            WireMock.stubFor(WireMock.request(method, WireMock.urlEqualTo(ERROR_URL_500))
                .willReturn(WireMock.aResponse()
                    .withStatus(500)));

            // 204 No Content
            WireMock.stubFor(WireMock.request(method, WireMock.urlEqualTo(NO_CONTENT_URL))
                .willReturn(WireMock.aResponse()
                    .withStatus(204)));
        }

        // Simple bogus URL that yields a 404
        WireMock.stubFor(WireMock.request("GET", WireMock.urlEqualTo(BOGUS_URL))
            .willReturn(WireMock.aResponse().withStatus(404)));

        // Timeout test
        WireMock.stubFor(WireMock.request("GET", WireMock.urlEqualTo(REMOTE_SLOW_URL))
            .willReturn(WireMock.aResponse().withFixedDelay(SLOW_TIMEOUT).withStatus(200)));


        pluginContext = Mockito.mock(PluginStepContext.class);
        pluginLogger = Mockito.mock(PluginLogger.class);
        Mockito.when(pluginContext.getLogger()).thenReturn(pluginLogger);

        dataContext = new HashMap<>();
        Mockito.when(pluginContext.getDataContext()).thenReturn(dataContext);

    }

    //@Test()
    public void canValidateConfiguration() {
        Map<String, Object> options = new HashMap<>();

        try {
            this.plugin.executeStep(pluginContext, options);
            fail("Expected configuration exception.");
        } catch (StepException se) {
            assertEquals(se.getFailureReason(), StepFailureReason.ConfigurationFailure);
        }

        options.put("remoteUrl", REMOTE_URL);
        options.put("method", "GET");

        try {
            this.plugin.executeStep(pluginContext, options);
            fail("Expected configuration exception.");
        } catch (StepException se) {
            assertEquals(se.getFailureReason(), StepFailureReason.ConfigurationFailure);
        }

        try {
            this.plugin.executeStep(pluginContext, options);
            fail("Expected configuration exception.");
        } catch (StepException se) {
            assertEquals(se.getFailureReason(), StepFailureReason.ConfigurationFailure);
        }
    }

    //@Test()
    public void canCallSimpleEndpoint() throws StepException {
        for (String method : Utils.HTTP_METHODS) {
            this.plugin.executeStep(pluginContext, this.getExecutionOptions(method));
        }
    }

    //@Test()
    public void canSetCustomTimeout() throws StepException {
        Map<String, Object> options = new HashMap<>();

        options.put("remoteUrl", Utils.BASE_URI + REMOTE_URL);
        options.put("method", "GET");
        options.put("timeout", REQUEST_TIMEOUT);

        this.plugin.executeStep(pluginContext, options);

        try {
            options.put("remoteUrl", Utils.BASE_URI + REMOTE_SLOW_URL);
            this.plugin.executeStep(pluginContext, options);
            fail("Expected exception " + StepException.class.getCanonicalName() + " not thrown.");
        } catch (StepException ignored) {
        }

        options.put("timeout", SLOW_TIMEOUT + 1000);
        this.plugin.executeStep(pluginContext, options);
    }

    //@Test()
    public void canCallBasicEndpoint() throws StepException {
        for (String method : Utils.HTTP_METHODS) {
            Map<String, Object> options = this.getBasicOptions(method);
            options.put("remoteUrl", BASE_URI_VALID);
            this.plugin.executeStep(pluginContext, options);
        }
    }

    //@Test(expected = StepException.class)
    public void canHandle500Error() throws StepException {
        Map<String, Object> options = new HashMap<>();

        options.put("remoteUrl", Utils.BASE_URI + ERROR_URL_500);
        options.put("method", "GET");

        this.plugin.executeStep(pluginContext, options);
    }

    //@Test(expected = StepException.class)
    public void canHandleBadUrl() throws StepException {
        Map<String, Object> options = new HashMap<>();

        options.put("remoteUrl", Utils.BASE_URI + BOGUS_URL);
        options.put("method", "GET");

        this.plugin.executeStep(pluginContext, options);
    }

    //@Test(expected = StepException.class)
    public void canHandleBadHost() throws StepException {
        Map<String, Object> options = new HashMap<>();

        options.put("remoteUrl", "https://neverGoingToBe.aProperUrl/bogus");
        options.put("method", "GET");

        this.plugin.executeStep(pluginContext, options);
    }

    //@Test
    public void canPrintNoContent() throws StepException {
        Map<String, Object> options = new HashMap<>();

        options.put("remoteUrl", Utils.BASE_URI + NO_CONTENT_URL);
        options.put("method", "GET");
        options.put("printResponse", true);
        options.put("printResponseToFile", false);

        this.plugin.executeStep(pluginContext, options);
    }
}
