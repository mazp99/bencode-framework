package com.mazepeng.codec;

public class IntegerUtils {
    private static final byte[] DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };
    private static final byte[] MIN_VALUE_BYTES = "-9223372036854775808".getBytes();

    public static int longToBytes(long value, byte[] buffer, int offset) {
        if (value == 0) {
            buffer[offset] = '0';
            return 1;
        }
        if (value == Long.MIN_VALUE) {
            System.arraycopy(MIN_VALUE_BYTES, 0, buffer, offset, MIN_VALUE_BYTES.length);
            return MIN_VALUE_BYTES.length;
        }

        int i = offset + 20;
        int start = i;
        long n = value;
        boolean negative = n < 0;
        if (negative) {
            n = -n;
        }

        while (n > 0) {
            buffer[--i] = DIGITS[(int) (n % 10)];
            n /= 10;
        }

        if (negative) {
            buffer[--i] = '-';
        }

        int len = start - i;
        System.arraycopy(buffer, i, buffer, offset, len);
        return len;
    }
}
