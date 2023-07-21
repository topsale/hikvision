package com.lusifer.hikvision.cache;

import com.lusifer.hikvision.beans.HikVisionCamera;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推流缓存信息
 */
public final class HikVisionCache {

    /**
     * 保存已经开始推的流
     */
    public static Map<String, HikVisionCamera> STREAM_MAP = new ConcurrentHashMap<>();

    /**
     * 保存服务启动时间
     */
    public static long START_TIME;

}
