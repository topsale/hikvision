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

    private String keepalive;

    private String pushIp;

    private String pushPort;

}
