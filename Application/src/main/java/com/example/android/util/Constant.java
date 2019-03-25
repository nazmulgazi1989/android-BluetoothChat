package com.example.android.util;

import java.nio.ByteBuffer;

public class Constant {
    public static final String STRING_PHOTO_PATH = "PhotoPath";
    public static final int PICK_IMAGE = 1234;
    public static final int PERMISSION_REQUEST_CODE = 5896;

    private static ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);

    public static byte[] longToBytes(int x) {
        buffer.putInt(0, x);
        return buffer.array();
    }

    public static int bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getInt();
    }
}
