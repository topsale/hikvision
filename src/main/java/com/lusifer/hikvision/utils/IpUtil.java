package com.lusifer.hikvision.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 工具类
 */
public class IpUtil {

    public static String convert(String domainName) {
        String ip = domainName;

        try {
            ip = InetAddress.getByName(domainName).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return domainName;
        }

        return ip;
    }

}
