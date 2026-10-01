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


public class ChatDemo {
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
        // 创建HTTP客户端
        HttpClient client = HttpClient.newHttpClient();
        // 创建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key)
                //逐行返回
                .POST(HttpRequest.BodyPublishers.ofString(order))
                .build();
        // 发送请求并获取响应
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));
//        // 打印响应状态码
//        System.out.println("Response status code: " + response.statusCode());
//        // 打印响应体
//        System.out.println(response.body());

        // 用于存储完整响应
        StringBuilder sb = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
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