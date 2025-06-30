package com.mazepeng.codec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 一个高性能、低内存占用的Bencode解码器。
 * 它直接从输入流解析数据，并将结果转换为Java的原生类型（Long, byte[], List, Map），
 * 以避免创建大量中间对象。
 *
 * @author Your Name
 */
public final class BencodeDecoder {

    private final PushbackInputStream in;
    private static final int MAX_BYTE_STRING_LENGTH = 8 * 1024 * 1024; // 8 MB 安全上限

    /**
     * 私有构造函数，防止外部直接实例化。
     * @param in 使用PushbackInputStream来支持字节“回退”。
     */
    private BencodeDecoder(InputStream in) {
        this.in = new PushbackInputStream(in);
    }

    /**
     * 解码Bencode数据的静态入口方法。
     * @param in 包含Bencode数据的输入流。
     * @return 解码后的对象，类型为Long, byte[], List<Object>, 或 Map<String, Object>。
     * @throws IOException 如果发生I/O错误或Bencode格式错误。
     */
    public static Object decode(InputStream in) throws IOException {
        return new BencodeDecoder(in).decodeNext();
    }

    /**
     * 核心的解码调度方法。
     * 它读取流中的下一个字节来判断数据类型，并分发给相应的解析方法。
     * @return 解析出的对象。
     * @throws IOException 格式或I/O错误。
     */
    private Object decodeNext() throws IOException {
        int firstByte = in.read();
        switch (firstByte) {
            case -1:
                // 流意外结束
                throw new EOFException("Bencode stream ended unexpectedly.");
            case 'i':
                return decodeInteger();
            case 'l':
                return decodeList();
            case 'd':
                return decodeDictionary();
            case 'e':
                // 'e' 是结束标记，不应该在解析一个新值的开头遇到它。
                throw new IOException("Unexpected 'e' marker at this position.");
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                // 如果是数字开头，说明是一个字节串。将这个数字退回流中。
                in.unread(firstByte);
                return decodeBytes();
            default:
                throw new IOException("Unknown Bencode type prefix: '" + (char)firstByte + "'");
        }
    }

    /**
     * 解析一个整数，格式为 i...e。
     * @return Long 类型的整数。
     * @throws IOException 格式或I/O错误。
     */
    private Long decodeInteger() throws IOException {
        StringBuilder sb = new StringBuilder();
        int currentByte;
        while ((currentByte = in.read()) != 'e') {
            if (currentByte == -1) throw new EOFException("Unterminated Bencoded integer.");
            sb.append((char) currentByte);
        }
        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            throw new IOException("Invalid integer format: " + sb, e);
        }
    }

    /**
     * 解析一个字节串，格式为 length:bytes。
     * @return byte[] 类型的字节数组。
     * @throws IOException 格式或I/O错误。
     */
    private byte[] decodeBytes() throws IOException {
        long length = readLength();
        if (length < 0) {
            throw new IOException("Byte string length cannot be negative: " + length);
        }
        if (length > MAX_BYTE_STRING_LENGTH) {
            throw new IOException("Byte string length exceeds safety limit: " + length);
        }

        int intLength = (int) length;
        byte[] bytes = new byte[intLength];
        int bytesRead = 0;

        while (bytesRead < intLength) {
            int readResult = in.read(bytes, bytesRead, intLength - bytesRead);
            if (readResult == -1) {
                throw new EOFException("Unexpected end of stream while reading byte string. " +
                        "Expected " + intLength + " bytes, but only got " + bytesRead);
            }
            bytesRead += readResult;
        }

        return bytes;
    }

    /**
     * 读取字节串的长度部分。
     * @return long 类型的长度。
     * @throws IOException 格式或I/O错误。
     */
    private long readLength() throws IOException {
        StringBuilder sb = new StringBuilder();
        int currentByte;
        while ((currentByte = in.read()) != ':') {
            if (currentByte == -1) throw new EOFException("Unterminated byte string length.");
            if (currentByte < '0' || currentByte > '9') {
                throw new IOException("Invalid character in byte string length: '" + (char)currentByte + "'");
            }
            sb.append((char) currentByte);
        }
        return Long.parseLong(sb.toString());
    }

    /**
     * 解析一个列表，格式为 l...e。
     * @return List<Object> 类型的列表。
     * @throws IOException 格式或I/O错误。
     */
    private List<Object> decodeList() throws IOException {
        List<Object> list = new ArrayList<>();
        while (true) {
            int peekByte = in.read();
            if (peekByte == -1) throw new EOFException("Unterminated Bencoded list.");

            if (peekByte == 'e') {
                // 列表结束，我们已经消耗了'e'，所以直接break。
                break;
            }

            // 退回窥视的字节，让 decodeNext 去解析
            in.unread(peekByte);
            list.add(decodeNext());
        }
        return list;
    }

    /**
     * 解析一个字典，格式为 d...e。
     * @return Map<String, Object> 类型的字典。
     * @throws IOException 格式或I/O错误。
     */
    private Map<String, Object> decodeDictionary() throws IOException {
        Map<String, Object> map = new TreeMap<>();
        while (true) {
            int peekByte = in.read();
            if (peekByte == -1) throw new EOFException("Unterminated Bencoded dictionary.");

            if (peekByte == 'e') {
                // 字典结束，消耗掉'e'并break。
                break;
            }

            in.unread(peekByte);

            Object keyObj = decodeNext();
            if (!(keyObj instanceof byte[])) {
                throw new IOException("Dictionary key must be a byte string, but got " + keyObj.getClass().getSimpleName());
            }
            String key = new String((byte[]) keyObj, StandardCharsets.UTF_8);

            Object value = decodeNext();
            map.put(key, value);
        }
        return map;
    }
}