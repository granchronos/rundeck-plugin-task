package com.example.webhook;

import com.dtolabs.rundeck.plugins.PluginLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"};
    public static final String BASE_URI = "http://localhost:18089";
    public static final String BASE_URI_VALID = "https://run.mocky.io/v3/6e376f0f-0a69-45c2-a654-10dfef4b2c15";

    public static HttpHeaders verifyHeadersAndSet(String headers, PluginLogger log) {

        Map<String, String> map;

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
            try {
                map = new ObjectMapper().readValue(headers, HashMap.class);
            } catch (Exception e) {
                map = null;
            }
        }

        if (map == null) {
            Yaml yaml = new Yaml();
            try {
                map = yaml.loadAs(headers, HashMap.class);
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

}
