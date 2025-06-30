# Bencode-Framework

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)

一个轻量级、高性能、零依赖的 Java Bencode 编码/解码库。专为需要处理 `.torrent` 文件。

## ✨ 特性

- **高性能**: 专注于最小化对象创建和内存分配，直接操作流以获得极致性能。
- **零依赖**: 纯Java实现，不依赖任何第三方库，易于集成。
- **原生类型支持**: 直接将Bencode数据解码为Java原生类型 (`Long`, `byte[]`, `List<Object>`, `Map<String, Object>`)，避免了不必要的封装对象。
- **流式处理**: 能够处理大型Bencode数据流，而无需将整个文件加载到内存中。
- **健壮性**: 包含对格式错误、超长数据等异常情况的安全检查。
- **符合规范**: 严格遵循Bencode规范，特别是对字典键按字典序排序的要求。

## 🚀 快速开始

### 1. 将项目添加到你的构建工具中

*(这里你可以根据你发布到Maven Central的方式来写，目前先用本地集成的示例)*

#### Maven
```xml
<dependency>
    <groupId>com.mazepeng</groupId>
    <artifactId>bencode-framework</artifactId>
    <version>0.0.1</version>
</dependency>
```

#### Gradle
```groovy
implementation 'com.mazepeng:bencode-framework:0.0.1'
```

### 2. 使用示例

#### 解码 (Decoding)

使用 `BencodeDecoder` 可以轻松地将一个 `InputStream` 解码为Java对象。

```java
import com.mazepeng.codec.BencodeDecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DecodeExample {
    public static void main(String[] args) throws IOException {
        String bencodedString = "d4:infod6:lengthi12345e4:name4:testee";
        InputStream in = new ByteArrayInputStream(bencodedString.getBytes());

        // 解码
        Object decoded = BencodeDecoder.decode(in);

        // 处理解码后的对象
        if (decoded instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) decoded;
            Map<String, Object> info = (Map<String, Object>) map.get("info");
            Long length = (Long) info.get("length");
            String name = new String((byte[]) info.get("name"));

            System.out.println("File Name: " + name);    // 输出: File Name: test
            System.out.println("File Length: " + length); // 输出: File Length: 12345
        }
    }
}
```

#### 编码 (Encoding)

使用 `BencodeEncoder` 可以将Java原生对象结构编码到 `OutputStream`。

```java
import com.mazepeng.codec.BencodeEncoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EncodeExample {
    public static void main(String[] args) throws IOException {
        // 为了符合Bencode规范（字典key按序），推荐使用TreeMap
        Map<String, Object> data = new TreeMap<>();
        data.put("name", "example");
        data.put("size", 1024L);
        data.put("files", List.of("a.txt", "b.jpg"));
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 编码
        BencodeEncoder.encode(data, out);

        byte[] encodedBytes = out.toByteArray();
        
        // Bencode是二进制格式，直接打印可能乱码，这里仅为演示
        System.out.println(new String(encodedBytes));
        // 输出: d5:filesl5:a.txt5:b.jpge4:name7:example4:sizei1024ee
    }
}
```

### ⚙️ API 概览

#### `com.mazepeng.codec.BencodeDecoder`

-   `public static Object decode(InputStream in) throws IOException`
    -   **功能**: 从输入流读取并解码Bencode数据。
    -   **返回**: 一个代表Bencode结构的对象。其运行时类型可能是：
        -   `Long` (对应 Bencode integer)
        -   `byte[]` (对应 Bencode byte string)
        -   `java.util.List<Object>` (对应 Bencode list)
        -   `java.util.Map<String, Object>` (对应 Bencode dictionary)

#### `com.mazepeng.codec.BencodeEncoder`

-   `public static void encode(Object obj, OutputStream out) throws IOException`
    -   **功能**: 将一个Java对象编码为Bencode格式，并写入输出流。
    -   **支持的对象类型 (`obj`)**:
        -   `String`
        -   `byte[]`
        -   `Number` (会被转换为 `long` 处理)
        -   `java.util.List<?>`
        -   `java.util.Map<String, ?>` (推荐使用 `TreeMap` 以保证key的顺序)

### 🤝 如何贡献

欢迎对本项目做出贡献！我们非常乐意接受您的 Pull Request。

1.  **Fork** 本仓库。
2.  创建一个新的分支 (`git checkout -b feature/your-feature-name`)。
3.  提交你的代码 (`git commit -am 'Add some feature'`)。
4.  将你的分支推送到远程仓库 (`git push origin feature/your-feature-name`)。
5.  创建一个新的 **Pull Request**。

请确保你的代码遵循现有的代码风格，并且所有测试都能通过。

### 📄 许可证

本项目采用 [MIT License](https://opensource.org/licenses/MIT) 许可证。