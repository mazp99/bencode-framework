package com.mazepeng.codec;

import com.mazepeng.codec.BencodeDecoder;
import com.mazepeng.codec.BencodeEncoder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DebugExample {
    public static void main(String[] args) throws IOException {
        // --- 1. 指定你的 .torrent 文件路径 ---
        // 请将 "path/to/your/ubuntu.torrent" 替换为你的实际文件路径
        String torrentFilePath = "/";

        try {
            System.out.println("--- 开始测试种子文件: " + torrentFilePath + " ---\n");

            // --- 2. 读取原始文件内容 ---
            Path path = Paths.get(torrentFilePath);
            if (!Files.exists(path)) {
                System.err.println("错误: 种子文件未找到，请检查路径: " + path.toAbsolutePath());
                return;
            }
            byte[] originalBytes = Files.readAllBytes(path);
            System.out.println("原始文件大小: " + originalBytes.length + " bytes");

            // --- 3. 使用 BencodeDecoder 解码 ---
            System.out.println("\n--- 步骤 1: 解码 ---");
            Object decodedObject;
            try (InputStream in = new FileInputStream(torrentFilePath)) {
                decodedObject = BencodeDecoder.decode(in);
            }
            System.out.println("解码成功！解码出的对象类型: " + decodedObject.getClass().getSimpleName());

            // --- 4. 美化打印解码后的数据结构 ---
            System.out.println("\n--- 步骤 2: 美化打印解码内容 ---");
            System.out.println(prettyPrint(decodedObject, 0));

            // --- 5. 使用 BencodeEncoder 重新编码 ---
            System.out.println("\n--- 步骤 3: 重新编码 ---");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BencodeEncoder.encode(decodedObject, baos);
            byte[] reEncodedBytes = baos.toByteArray();
            System.out.println("重新编码成功！编码后大小: " + reEncodedBytes.length + " bytes");

            // --- 6. 验证往返一致性 ---
            System.out.println("\n--- 步骤 4: 验证一致性 ---");
            if (Arrays.equals(originalBytes, reEncodedBytes)) {
                System.out.println("成功: 重新编码后的字节与原始文件完全一致！编码器和解码器工作正常。");
            } else {
                System.err.println("失败: 重新编码后的字节与原始文件不一致！");
            }

        } catch (IOException e) {
            System.err.println("\n处理过程中发生IO异常:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n处理过程中发生未知异常:");
            e.printStackTrace();
        }
    }
    public static String prettyPrint(Object obj, int indentLevel) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            indent.append("  ");
        }

        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{\n");
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sb.append(indent).append("  \"").append(entry.getKey()).append("\": ");
                sb.append(prettyPrint(entry.getValue(), indentLevel + 1));
                sb.append(",\n");
            }
            if (!map.isEmpty()) {
                sb.setLength(sb.length() - 2); // 移除最后一个逗号和换行符
            }
            sb.append("\n").append(indent).append("}");
            return sb.toString();
        } else if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[\n");
            List<Object> list = (List<Object>) obj;
            for (Object item : list) {
                sb.append(indent).append("  ").append(prettyPrint(item, indentLevel + 1));
                sb.append(",\n");
            }
            if (!list.isEmpty()) {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n").append(indent).append("]");
            return sb.toString();
        } else if (obj instanceof Long) {
            return obj.toString();
        } else if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            try {
                for (byte b : bytes) {
                    if (b < 32 && b != '\n' && b != '\r' && b != '\t') {
                        return "\"<binary " + bytes.length + " bytes>\"";
                    }
                }
                return "\"" + new String(bytes, StandardCharsets.UTF_8) + "\"";
            } catch (Exception e) {
                return "\"<binary " + bytes.length + " bytes>\"";
            }
        } else {
            return "null";
        }
    }
}
