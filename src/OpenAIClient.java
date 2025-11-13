import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OpenAIClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String perguntar(String prompt) throws Exception {
        String apiKey = AppConfig.getOpenAIApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("openai.api.key não definido em config/app.properties");
        }

        String jsonBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erro na API OpenAI: " + response.statusCode()
                    + " - " + response.body());
        }

        String body = response.body();
        return extrairTextoDaResposta(body);
    }

    private String buildRequestBody(String prompt) {
        String escapedPrompt = escapeJson(prompt);
        String model = AppConfig.getOpenAIModel();

        return """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "Você é um assistente em português que responde de forma clara, organizada e objetiva usando apenas o contexto enviado."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "temperature": 0.3
                }
                """.formatted(model, escapedPrompt);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // Parser simples: pega o primeiro "content":"...".
    // Pra produção, o ideal é usar uma lib JSON (Gson/Jackson).
    private String extrairTextoDaResposta(String json) {
        // Procura o campo "content":"..."
        String marker = "\"content\":";
        int idx = json.indexOf(marker);
        if (idx == -1) {
            return json;
        }

        // Avança até a aspa inicial
        int start = json.indexOf('"', idx + marker.length());
        if (start == -1) return json;

        start++; // pula a aspa
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                // Fechou o campo content
                break;
            }

            sb.append(c);
        }

        return sb.toString().trim();
    }
}
