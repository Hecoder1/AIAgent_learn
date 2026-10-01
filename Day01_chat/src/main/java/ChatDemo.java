import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


public class ChatDemo {
    // 用于存储完整响应
    static StringBuilder sb = new StringBuilder();
    static ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) throws IOException, InterruptedException {
        // 获取环境变量中的API密钥
        String key=System.getenv("DEEPSEEK_API_KEY");
        // 检查API密钥是否设置
        if(key==null || key.isEmpty()){
            System.out.println("DEEPSEEK_API_KEY is not set");
            return;
        }
        // 定义请求体
        String order = "{\"model\":\"deepseek-flash\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}";
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                askDeepSeek(key, order);
                break;      // 成功
            } catch (IOException e) {
                if (sb.length() > 0) {
                    System.out.println("\n回答传了一半断了，为避免重复输出，不重试");
                    return;
                }
                if (attempt == 3) { System.out.println("3 次全失败：" + e); return; }
                long wait = 1000L << (attempt - 1);   // 1000 → 2000 → 4000 毫秒
                System.out.println("第 " + attempt + " 次失败，" + wait + "ms 后重试…");
                Thread.sleep(wait);
            }
        }


    }

    private static void askDeepSeek(String key, String order) throws IOException, InterruptedException {
        // 创建HTTP客户端(设置超时时间)
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        // 创建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key)
                // 设置请求超时时间
                .timeout(Duration.ofSeconds(30))
                //逐行返回
                .POST(HttpRequest.BodyPublishers.ofString(order))
                .build();
        // 发送请求并获取响应
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // 检查响应状态码
        int code = response.statusCode();
        if (code == 429 || code >= 500) {
            throw new IOException("HTTP " + code);   // 服务器临时不接待 → 抛出去让重试循环接住
        }
        if (code != 200) {
            System.out.println("HTTP " + code + "（401=key错 402=欠费 400=订单写错，重试没用）："
                    + new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
            return;
        }

        // 读取响应体
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));
//        // 打印响应状态码
//        System.out.println("Response status code: " + response.statusCode());
//        // 打印响应体
//        System.out.println(response.body());


//        // 提取初始响应内容
//        String content = root.path("choices")
//                .get(0)
//                .path("message")
//                .path("content")
//                .asText();

        // 逐行读取响应体
        String line;
        while ((line = reader.readLine()) != null) {
            if(line.isEmpty()){
                continue;
            }
            if(!line.startsWith("data:")){
                continue;
            }
            String data = line.substring(5).trim();
            if(data.equals("[DONE]")) {
                break;
            }
            // 解析当前行数据
            JsonNode node = mapper.readTree(data);
            JsonNode contentNode = node.path("choices").path(0).path("delta").path("content");
            if (contentNode.isTextual()) {
                String piece = contentNode.asText();
                System.out.print(piece);
                System.out.flush();
                sb.append(piece);
            }

        }
    }
}