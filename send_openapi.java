///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.0
//DEPS org.apache.httpcomponents.client5:httpclient5:5.2.1
//DEPS org.yaml:snakeyaml:2.0

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.yaml.snakeyaml.Yaml;

public class send_openapi {
    static final String API_ENDPOINT = System.getenv("API_ENDPOINT");

    public static void main(String[] args) throws IOException {
        if (API_ENDPOINT == null || API_ENDPOINT.isEmpty()) {
            System.err.println("❌ ERRO: API has not been defined!");
            System.exit(1);
        } else {
            System.out.println("🟢 ENDPONT: " + API_ENDPOINT);
        }

        List<Path> openApiFiles = findOpenApiFiles(Paths.get("."));
        if (openApiFiles.isEmpty()) {
            System.out.println("ℹ️ No open api spec or swagger file found.");
            return;
        }

        for (Path file : openApiFiles) {
            System.out.println("📤 Sending ...: " + file);
            sendFileToApi(file);
        }

        System.out.println("✅ All API Spec files were sent successfully.");
    }

    static List<Path> findOpenApiFiles(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".yaml"))
                        .filter(send_openapi::isValidOpenApi) // 
                        .collect(Collectors.toList());
        }
    }

    static boolean isValidOpenApi(Path filePath) {
        try {
            String content = Files.readString(filePath);
            JsonNode rootNode = parseToJson(content, filePath.toString());

            if (rootNode == null) return false;

            // 🚀 Valida presença de chaves essenciais
            if (rootNode.has("openapi") || rootNode.has("swagger")) {
                if (rootNode.has("info") && rootNode.has("paths")) {
                    System.out.println("✔️ Detected OpenAPI: " + filePath);
                    return true;
                }
            }

            System.out.println("❌ It's not: " + filePath);
            return false;
        } catch (Exception e) {
            System.out.println("❌ Error processing " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    static JsonNode parseToJson(String content, String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (fileName.endsWith(".yaml")) {
                Yaml yaml = new Yaml();
                Object yamlObj = yaml.load(content);
                return objectMapper.valueToTree(yamlObj); // Converte YAML → JSON
            } else {
                return objectMapper.readTree(content); // JSON normal
            }
        } catch (Exception e) {
            System.out.println("❌ Fail converting " + fileName + " to JSON: " + e.getMessage());
            return null;
        }
    }

    static void sendFileToApi(Path filePath) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String content = Files.readString(filePath);
            HttpPost request = new HttpPost(API_ENDPOINT);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(content));

            try (var response = client.execute(request)) {
                System.out.println("➡️ " + filePath + " -> Status: " + response.getCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error on sending " + filePath + ": " + e.getMessage());
        }
    }
}
// API_ENDPOINT = https://webhook.site/06447631-b5b2-43a9-9f7d-028f0d3a4618 

// noovo