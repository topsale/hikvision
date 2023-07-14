package com.lusifer.hikvision.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类
 */
public final class MD5Utils {

    /**
     * MD5 加密
     *
     * @return 32 位字符串
     */
    public static String encrypt32(String str) {
        if (str == null) {
            return null;
        }
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] b = md.digest();
            int i;
            StringBuilder buf = new StringBuilder("");
            for (byte value : b) {
                i = value;
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * MD5 加密
     *
     * @return 返回 16 位的字符串(16 位的密文就是 32 位中间的 16 位)
     */
    public static String encrypt16(String str) {
        if (str == null) {
            return null;
        }
        return encrypt32(str).substring(8, 24);
    }

}
