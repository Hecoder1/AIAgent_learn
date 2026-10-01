import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


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
                .POST(HttpRequest.BodyPublishers.ofString(order))
                .build();
        // 发送请求并获取响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//        // 打印响应状态码
//        System.out.println("Response status code: " + response.statusCode());
//        // 打印响应体
//        System.out.println(response.body());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        String content = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        System.out.println(content);

    }
}
