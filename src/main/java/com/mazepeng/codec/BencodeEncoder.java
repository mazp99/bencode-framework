package com.mazepeng.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BencodeEncoder {
    // --- 预计算的常量字节 ---
    private static final byte INTEGER_PREFIX = 'i';
    private static final byte LIST_PREFIX = 'l';
    private static final byte DICTIONARY_PREFIX = 'd';
    private static final byte END_SUFFIX = 'e';
    private static final byte SEPARATOR = ':';

    // 线程本地的缓冲区，避免每次都创建。21字节足够容纳任何long的字符串形式。
    private static final ThreadLocal<byte[]> NUMBER_BUFFER = ThreadLocal.withInitial(() -> new byte[21]);

    // --- 公开的编码方法 ---

    public static void encode(Object obj, OutputStream out) throws IOException {
        if (obj instanceof String) {
            encode((String) obj, out);
        } else if (obj instanceof byte[]) {
            encode((byte[]) obj, out);
        } else if (obj instanceof Number) {
            encode(((Number) obj).longValue(), out);
        } else if (obj instanceof List) {
            encode((List<?>) obj, out);
        } else if (obj instanceof Map) {
            // 为了Bencode规范，必须是TreeMap或者在编码前排序
            if (obj instanceof TreeMap) {
                encode((Map<String, ?>) obj, out);
            } else {
                encode(new TreeMap<>((Map<String, ?>) obj), out);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type for Bencode encoding: " + obj.getClass());
        }
    }

    // --- 各种类型的私有实现 ---

    public static void encode(String str, OutputStream out) throws IOException {
        encode(str.getBytes(StandardCharsets.UTF_8), out);
    }

    public static void encode(byte[] bytes, OutputStream out) throws IOException {
        byte[] buffer = NUMBER_BUFFER.get();
        int len = IntegerUtils.longToBytes(bytes.length, buffer, 0);
        out.write(buffer, 0, len);
        out.write(SEPARATOR);
        out.write(bytes);
    }

    public static void encode(long value, OutputStream out) throws IOException {
        out.write(INTEGER_PREFIX);
        byte[] buffer = NUMBER_BUFFER.get();
        int len = IntegerUtils.longToBytes(value, buffer, 0);
        out.write(buffer, 0, len);
        out.write(END_SUFFIX);
    }

    public static void encode(List<?> list, OutputStream out) throws IOException {
        out.write(LIST_PREFIX);
        for (Object item : list) {
            encode(item, out);
        }
        out.write(END_SUFFIX);
    }

    public static void encode(Map<String, ?> map, OutputStream out) throws IOException {
        // 假设传入的已经是排好序的Map (如TreeMap)
        out.write(DICTIONARY_PREFIX);
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            encode(entry.getKey(), out); // Key必须是String
            encode(entry.getValue(), out);

        }
        out.write(END_SUFFIX);
    }
}

