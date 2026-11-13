package com.xingguang.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高德地理编码（REST）：把文字地址转成经纬度，供配送地图打点。
 *
 * <p>失败一律返回 null（不阻塞下单/详情等主流程）；成功结果进程内缓存，避免重复请求。
 * <p>Key 走配置 {@code xingguang.amap.key}（Web 服务 REST 类型 Key）。
 */
@Service
public class AmapGeocodeService {

    private static final Logger log = LoggerFactory.getLogger(AmapGeocodeService.class);

    private static final String API = "https://restapi.amap.com/v3/geocode/geo";

    @Value("${xingguang.amap.key:}")
    private String key;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    /** address -> [lng, lat]；仅缓存成功结果 */
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    /**
     * 地址 → [lng, lat]；Key 未配置 / 无结果 / 网络异常均返回 null。
     */
    public double[] geocode(String address) {
        if (key == null || key.isBlank() || address == null || address.isBlank()) {
            return null;
        }
        String addr = address.trim();
        double[] cached = cache.get(addr);
        if (cached != null) {
            return cached;
        }
        double[] result = fetch(addr);
        if (result != null) {
            cache.put(addr, result);
        }
        return result;
    }

    private double[] fetch(String address) {
        try {
            String url = API + "?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(resp.body());
            if (!"1".equals(root.path("status").asText())) {
                log.warn("[地理编码] 失败 address={} info={}", address, root.path("info").asText());
                return null;
            }
            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty()) {
                return null;
            }
            // location 形如 "117.701234,39.003456"
            String location = geocodes.get(0).path("location").asText();
            String[] parts = location.split(",");
            if (parts.length != 2) {
                return null;
            }
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
        } catch (Exception e) {
            log.warn("[地理编码] 异常 address={} err={}", address, e.getMessage());
            return null;
        }
    }
}
