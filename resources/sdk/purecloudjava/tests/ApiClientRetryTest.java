
package com.mypurecloud.sdk.v2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Stopwatch;
import com.mypurecloud.sdk.v2.*;
import com.mypurecloud.sdk.v2.connector.*;
import com.mypurecloud.sdk.v2.extensions.AuthResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;

public class ApiClientRetryTest {

    private ApiClient apiClient;

    private String environment;
    private PureCloudRegionHosts region;
    private boolean useenum = true;
    Stopwatch stopwatch;

    @Mock
    ApiClientConnector connector;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void invokeTestWith_429() throws IOException {
        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();

        retryConfiguration.setMaxRetryTime(6);
        retryConfiguration.setDefaultDelay(100);

        apiClient = getApiClient(retryConfiguration);

        Map<String, String> headers = new HashMap<>();
        headers.put("Retry-After", "3");

        try {
            when(connector.invoke(any(ApiClientConnectorRequest.class))).thenReturn(getConnectorResponse(429, headers));

            stopwatch = Stopwatch.createStarted();
            ApiResponse<ApiClientConnectorResponse> response = apiClient.invoke(getConnectorRequest(), getReturnType());
        } catch (ApiException ex) {
            Assert.assertEquals(429, ex.getStatusCode());
            Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) > 6000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 6100, "It will wait for every 100 Mills and retry until 6 Seconds");
            stopwatch.stop();
        }
    }

    @Test
    public void invokeTestWith_429_And_No_MaxRetryTime() throws IOException {
        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        apiClient = getApiClient(retryConfiguration);

        Map<String, String> headers = new HashMap<>();
        headers.put("Retry-After", "3");

        try {
            when(connector.invoke(any(ApiClientConnectorRequest.class))).thenReturn(getConnectorResponse(429, headers));

            stopwatch = Stopwatch.createStarted();
            ApiResponse<ApiClientConnectorResponse> response = apiClient.invoke(getConnectorRequest(), getReturnType());
        } catch (ApiException ex) {
            Assert.assertEquals(429, ex.getStatusCode());
            Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 0 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 100, "Since maxRetryTime is not provided it will not retry even if the status code is 429");
            stopwatch.stop();
        }
    }

    @Test
    public void invokeTestWith_502() throws IOException {
        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();

        retryConfiguration.setMaxRetryTime(13);
        retryConfiguration.setDefaultDelay(2000);
        retryConfiguration.setBackoffInterval(11000);

        apiClient = getApiClient(retryConfiguration);

        try {
            when(connector.invoke(any(ApiClientConnectorRequest.class))).thenReturn(getConnectorResponse(502, Collections.emptyMap()));

            stopwatch = Stopwatch.createStarted();
            ApiResponse<ApiClientConnectorResponse> response = apiClient.invoke(getConnectorRequest(), getReturnType());
        } catch (ApiException ex) {
            Assert.assertEquals(502, ex.getStatusCode());
            Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) > 13000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 13100L, "It will wait for every 2 Sec and retry for 5 times then it will backoff for 3 sec and retry then it exits.");
            stopwatch.stop();
        }
    }

    @Test
    public void invokeTestWith_503() throws IOException {
        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();

        retryConfiguration.setMaxRetryTime(40);
        retryConfiguration.setDefaultDelay(200);
        retryConfiguration.setBackoffInterval(3000);

        apiClient = getApiClient(retryConfiguration);

        try {
            when(connector.invoke(any(ApiClientConnectorRequest.class))).thenReturn(getConnectorResponse(503, Collections.emptyMap()));

            stopwatch = Stopwatch.createStarted();
            ApiResponse<ApiClientConnectorResponse> response = apiClient.invoke(getConnectorRequest(), getReturnType());
        } catch (ApiException ex) {
            Assert.assertEquals(503, ex.getStatusCode());
            Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) > 40000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 40100, "It will wait for every 200 Mills and retry for 5 times then it will backoff for 3 Sec once, 9 Sec once and 27 Sec before retrying");
            stopwatch.stop();
        }
    }


    @Test
    public void invokeTestWith_504() throws IOException {
        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();

        retryConfiguration.setMaxRetryTime(3);

        apiClient = getApiClient(retryConfiguration);

        try {
            when(connector.invoke(any(ApiClientConnectorRequest.class))).thenReturn(getConnectorResponse(504, Collections.emptyMap()));

            stopwatch = Stopwatch.createStarted();
            ApiResponse<ApiClientConnectorResponse> response = apiClient.invoke(getConnectorRequest(), getReturnType());
        } catch (ApiException ex) {
            Assert.assertEquals(504, ex.getStatusCode());
            Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) > 3000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 3100, "It will wait for every 200 Mills and retry for 5 times then it will backoff for 3 Sec once, 9 Sec once and 27 Sec before retrying");
            stopwatch.stop();
        }
    }

    @Test
    public void invokeTestWith_504_No_MaxRetryTime() throws IOException {
        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();

        apiClient = getApiClient(retryConfiguration);

        try {
            when(connector.invoke(any(ApiClientConnectorRequest.class))).thenReturn(getConnectorResponse(504, Collections.emptyMap()));

            stopwatch = Stopwatch.createStarted();
            ApiResponse<ApiClientConnectorResponse> response = apiClient.invoke(getConnectorRequest(), getReturnType());
        } catch (ApiException ex) {
            Assert.assertEquals(504, ex.getStatusCode());
            Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 0 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 100, "Since maxRetryTime is not provided it will not retry even if the status code is 504");
            stopwatch.stop();
        }
    }

    @Test
    public void shouldRetryTestWith_200() throws IOException {

        ApiClientConnectorResponse response = getConnectorResponse(200, Collections.emptyMap());

        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        retryConfiguration.setMaxRetryTime(30);

        ApiClient.Retry retry = new ApiClient.Retry(retryConfiguration);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = retry.shouldRetry(response);
        Assert.assertFalse(result, "Status Code is 200, so shouldRetry method returns false");
        Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 0 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 100);
        stopwatch.stop();
    }

    @Test
    public void shouldRetryTestWith_429() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Retry-After", "1");

        ApiClientConnectorResponse response = getConnectorResponse(429, headers);

        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        retryConfiguration.setMaxRetryTime(30);

        ApiClient.Retry retry = new ApiClient.Retry(retryConfiguration);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = retry.shouldRetry(response);
        Assert.assertTrue(result, "Status Code is 429, so it will sleep for 1 Sec as provided in Retry-After header and returns true");
        Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 1000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 1100);
        stopwatch.stop();

    }

    @Test
    public void shouldRetryTestWith_502() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("Retry-After", "1");

        ApiClientConnectorResponse response = getConnectorResponse(502, headers);

        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        retryConfiguration.setMaxRetryTime(30);

        ApiClient.Retry retry = new ApiClient.Retry(retryConfiguration);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = retry.shouldRetry(response);
        Assert.assertTrue(result, "Status Code is 502, so it will sleep for 1 Sec as provided in Retry-After header and returns true");
        Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 1000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 1100);
        stopwatch.stop();
    }

    @Test
    public void shouldRetryTestWith_502_And_0_MaxRetryTime() throws IOException {

        ApiClientConnectorResponse response = getConnectorResponse(502, Collections.emptyMap());

        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        Stopwatch stopwatch = Stopwatch.createStarted();
        ApiClient.Retry retry = new ApiClient.Retry(retryConfiguration);
        boolean result = retry.shouldRetry(response);
        Assert.assertFalse(result, "Even though Status Code is 502, it will return false because MaxRetryTime is set to Zero by default");
        Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 0 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 100);
        stopwatch.stop();
    }

    @Test
    public void shouldRetryTestWith_503_And_RetryConfig() throws IOException {

        ApiClientConnectorResponse response = getConnectorResponse(503, Collections.emptyMap());

        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        retryConfiguration.setBackoffInterval(6000L);
        retryConfiguration.setDefaultDelay(3000L);
        retryConfiguration.setMaxRetryTime(10);
        retryConfiguration.setMaxRetriesBeforeBackoff(0);

        ApiClient.Retry retry = new ApiClient.Retry(retryConfiguration);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = retry.shouldRetry(response);
        Assert.assertTrue(result, "Since Status Code is 503 and maxRetriesBeforeBackoff is Zero, backoff block will be executed and returns true");
        Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 3000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 3100);
        stopwatch.stop();
    }

    @Test
    public void shouldRetryTestWith_504_And_No_RetryAfter_Header() throws IOException {

        ApiClientConnectorResponse response = getConnectorResponse(504, Collections.emptyMap());

        ApiClient.RetryConfiguration retryConfiguration = new ApiClient.RetryConfiguration();
        retryConfiguration.setMaxRetryTime(30);
        ApiClient.Retry retry = new ApiClient.Retry(retryConfiguration);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = retry.shouldRetry(response);
        Assert.assertTrue(result, "Even though Retry-After header is missing, it will sleep for 3 Sec by default and returns true");
        Assert.assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 3000 && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 3100);
        stopwatch.stop();
    }

    private ApiClient getApiClient(ApiClient.RetryConfiguration retryConfiguration) {
        region = getEnvironment();
        if (region == null) {
            useenum = false;
        }
        try {
            ApiClient.Builder builder = ApiClient.Builder.standard();
            builder = builder.withProperty(ApiClientConnectorProperty.CONNECTOR_PROVIDER, connector);
            if (useenum) {
                builder = builder.withBasePath(region);
            } else {
                builder = builder.withBasePath("https://api." + environment);
            }
            builder = builder.withRetryConfiguration(retryConfiguration);
            apiClient = builder.build();

            ApiResponse<AuthResponse> authResponse = apiClient.authorizeClientCredentials(getClientId(), getClientSecret());

            Configuration.setDefaultApiClient(apiClient);

        } catch (ApiException ex) {
            handleApiException(ex);
        } catch (Exception ex) {
            System.out.println(ex);
            Assert.fail();
        }
        return apiClient;

    }

    private ApiClientConnectorResponse getConnectorResponse(int statusCode, Map<String, String> headers) {
        return new ApiClientConnectorResponse() {
            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getStatusReasonPhrase() {
                return null;
            }

            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }

            @Override
            public boolean hasBody() {
                return false;
            }

            @Override
            public String readBody() throws IOException {
                return null;
            }

            @Override
            public InputStream getBody() throws IOException {
                return null;
            }

            @Override
            public void close() throws Exception {

            }
        };
    }

    private ApiRequest<?> getConnectorRequest() {
        return new ApiRequest<ApiClientConnectorRequest>() {
            @Override
            public String getPath() {
                return "/api/v2/users" ;
            }

            @Override
            public String getMethod() {
                return "GET" ;
            }

            @Override
            public Map<String, String> getPathParams() {
                return Collections.emptyMap();
            }

            @Override
            public List<Pair> getQueryParams() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> getFormParams() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> getHeaderParams() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> getCustomHeaders() {
                return Collections.emptyMap();
            }

            @Override
            public String getContentType() {
                return "application/json" ;
            }

            @Override
            public String getAccepts() {
                return "application/json" ;
            }

            @Override
            public ApiClientConnectorRequest getBody() {
                return null;
            }

            @Override
            public String[] getAuthNames() {
                return new String[]{"PureCloud OAuth"};
            }
        };
    }

    private TypeReference<ApiClientConnectorResponse> getReturnType() {
        return new TypeReference<ApiClientConnectorResponse>() {
            @Override
            public Type getType() {
                return super.getType();
            }
        };
    }

    private PureCloudRegionHosts getEnvironment() {
        environment = System.getenv("PURECLOUD_ENVIRONMENT");
        switch (environment) {
            case "mypurecloud.com":
                return PureCloudRegionHosts.us_east_1;
            case "mypurecloud.ie":
                return PureCloudRegionHosts.eu_west_1;
            case "mypurecloud.com.au":
                return PureCloudRegionHosts.ap_southeast_2;
            case "mypurecloud.jp":
                return PureCloudRegionHosts.ap_northeast_1;
            case "mypurecloud.de":
                return PureCloudRegionHosts.eu_central_1;
            default:
                System.out.println("Not in PureCloudRegionHosts using string value");
                return null;
        }
    }

    private String getClientId() {
        return System.getenv("PURECLOUD_CLIENT_ID");
    }

    private String getClientSecret() {
        return System.getenv("PURECLOUD_CLIENT_SECRET");
    }

    private void handleApiException(ApiException ex) {
        System.out.println(ConsoleColors.applyTag(ConsoleColors.RED_BOLD, "API Exception") +
                "(" + ex.getCorrelationId() + ") " +
                ex.getStatusCode() + " " + ex.getStatusReasonPhrase() + " - " + ex.getRawBody());
        Assert.fail(ex.getStatusCode() + " " + ex.getStatusReasonPhrase() + " (" + ex.getCorrelationId() + ")");
    }
}