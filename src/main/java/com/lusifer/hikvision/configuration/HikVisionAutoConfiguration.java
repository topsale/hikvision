package com.lusifer.hikvision.configuration;

import com.lusifer.hikvision.sdk.HCNetSDK;
import com.lusifer.hikvision.utils.OSSelectUtil;
import com.lusifer.hikvision.utils.SpringContextHolder;
import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;

/**
 * 海康威视自动配置类
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({HikVisionProperties.class})
@Import(SpringContextHolder.class)
public class HikVisionAutoConfiguration {

    private static final String WIN_HIK_HCNetSDK = "HCNetSDK.dll";
    private static final String WIN_HIK_PlayCtrl = "PlayCtrl.dll";

    /**
     * 初始化配置
     */
    public HikVisionAutoConfiguration(HikVisionProperties properties) {
        if (properties.getPath() == null) {
            throw new IllegalArgumentException("请指定 SDK 类库，注意区分系统版本");
        }
    }

    /**
     * 初始化 HCNetSDK
     */
    @Bean
    public HCNetSDK initHCNetSDK(HikVisionProperties properties) {
        HCNetSDK sdk = null;

        synchronized (HCNetSDK.class) {
            String sdkPath = "";

            try {
                // 加载 windows 库路径
                if (OSSelectUtil.isWindows()) {
                    sdkPath = properties.getPath().startsWith("classpath") ?
                            ResourceUtils.getFile(properties.getPath() + "/".concat(WIN_HIK_HCNetSDK)).getPath() : properties.getPath() + "/".concat(WIN_HIK_HCNetSDK);
                }

                sdk = Native.load(sdkPath, HCNetSDK.class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                log.error("HCNetSDK: 加载失败");
            }
        }

        return sdk;
    }

}
