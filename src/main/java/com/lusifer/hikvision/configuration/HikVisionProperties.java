package com.lusifer.hikvision.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 海康威视配置类
 */
@Data
@ConfigurationProperties(prefix = "hik")
public class HikVisionProperties {

    /**
     * SDK 配置路径
     */
    private String path;

    /**
     * 拉流配置
     */
    public PullStream stream;

    /**
     * 线程池配置
     */
    private HikPool pool;

    /**
     * 线程池
     */
    @Data
    public static class HikPool {

        /**
         * 核心线程数
         */
        private int corePoolSize = 10;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 200;

        /**
         * 队列大小
         */
        private int queueCapacity = 1024;

        /**
         * 空闲线程活跃时间（秒)
         */
        private int keepAliveSeconds = 60;

    }

    /**
     * M3U8 推流
     * <p>
     * M3U8 是是一种纯文本文件， 使用用 UTF-8 编码。
     * 本质上一个播放列表。文件中存放了视频的基本信息和分段视频的索引地址 (将一整个视频分成了时长不同的很多小段)。
     * 当播放 m3u8 视频时，就是按顺序下载播放索引列表的视频，从而完成一部完整视频的播放。
     * 是苹果公司使用的 HTTP Live Streaming（HLS）协议格式的基础。
     * </p>
     */
    @Data
    public static class PullStream {

        /**
         * M3U8 盘路径
         */
        private String path;

    }

}
