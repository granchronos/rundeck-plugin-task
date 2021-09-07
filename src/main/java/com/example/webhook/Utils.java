package com.example.webhook;

import com.dtolabs.rundeck.plugins.PluginLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"};
    public static final String BASE_URI = "http://localhost:18089";

    public static HttpHeaders verifyHeadersAndSet(String headers, PluginLogger log) {

        Map<String, String> map = new HashMap<>();

        // Example: "a=1,b=2,c=3=44=5555"
        try {
            map = Arrays
                    .stream(headers.split(","))
                    .map(s -> s.split("="))
                    .collect(Collectors.toMap(s -> s[0], s -> s[1]));
        } catch (Exception e) {
            map = null;
        }

        // Example: "{"field1":"value1","field2":"value2"}"
        if (map == null) {
            map = new HashMap<>();
            try {
                map = new ObjectMapper().readValue(headers, HashMap.class);
            } catch (Exception e) {
                map = null;
            }
        }

        if (map == null) {
            Yaml yaml = new Yaml();
            map = new HashMap<>();
            try {
                map = yaml.loadAs(headers, HashMap.class);
            } catch (Exception e) {
                map = null;
            }
        }

        if (map == null) {
            map = new HashMap<>();
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(headers)));
                if (doc.hasAttributes()) {
                    NamedNodeMap namedNodeMap = doc.getAttributes();
                    for (int i = 0; i < namedNodeMap.getLength(); i++) {
                        Node tempNode = namedNodeMap.item(i);
                        String value = tempNode.getNodeValue();
                        if (value != null) {
                            map.put(tempNode.getNodeName(), value);
                        }
                    }
                }


            } catch (Exception e) {
                map = null;
            }
        }

        if (map == null) {
            log.log(5, "Error parsing the headers");
            return null;
        } else {
            MultiValueMap<String, String> headersToSet = new LinkedMultiValueMap<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                headersToSet.add(entry.getKey(), entry.getValue());
            }
            return HttpHeaders.readOnlyHttpHeaders(headersToSet);
        }
    }

    public static RestTemplate restTemplate(RestTemplateBuilder builder) throws GeneralSecurityException {
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
