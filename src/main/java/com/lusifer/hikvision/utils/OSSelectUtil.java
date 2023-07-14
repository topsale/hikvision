package com.lusifer.hikvision.utils;

/**
 * 操作系统判断工具
 */
public class OSSelectUtil {

    /**
     * 判断是否是 Linux
     */
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    /**
     * 判断是否是 Windows
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

}
