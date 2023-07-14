package com.lusifer.hikvision.configuration;

import com.lusifer.hikvision.sdk.HCNetSDK;
import com.lusifer.hikvision.sdk.HCPlaySDK;
import com.lusifer.hikvision.utils.OSSelectUtil;
import com.lusifer.hikvision.utils.SpringContextHolder;
import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 海康威视自动配置类
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({HikVisionProperties.class})
@Import(SpringContextHolder.class)
public class HikVisionAutoConfiguration {

    /**
     * Windows HCNetSDK
     */
    private static final String WIN_HIK_HCNetSDK = "HCNetSDK.dll";

    /**
     * Windows HCPlaySDK
     */
    private static final String WIN_HIK_HCPlaySDK = "PlayCtrl.dll";

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
        HCNetSDK hCNetSDK = null;

        synchronized (HCNetSDK.class) {
            String hCNetSDKPath = "";

            try {
                // 加载 Windows 库路径
                if (OSSelectUtil.isWindows()) {
                    hCNetSDKPath = properties.getPath().startsWith("classpath") ?
                            ResourceUtils.getFile(properties.getPath() + "/".concat(WIN_HIK_HCNetSDK)).getPath() : properties.getPath() + "/".concat(WIN_HIK_HCNetSDK);
                }

                // 加载类库
                hCNetSDK = (HCNetSDK) Native.loadLibrary(hCNetSDKPath, HCNetSDK.class);
            } catch (FileNotFoundException e) {
                log.error("加载 HCNetSDK 失败");
                e.printStackTrace();
                return null;
            }
        }

        log.info("加载 HCNetSDK 成功");
        return hCNetSDK;
    }

    /**
     * 初始化 HCPlaySDK，仅 Web 模式下
     */
    @Bean
    @ConditionalOnWebApplication
    public HCPlaySDK initHCPlaySDK(HikVisionProperties properties) {
        HCPlaySDK hCPlaySDK = null;

        synchronized (HCPlaySDK.class) {
            String hCPlaySDKPath = "";

            try {
                // 加载 Windows 库路径
                if (OSSelectUtil.isWindows()) {
                    hCPlaySDKPath = properties.getPath().startsWith("classpath") ?
                            ResourceUtils.getFile(properties.getPath() + "/".concat(WIN_HIK_HCPlaySDK)).getPath() : properties.getPath() + "/".concat(WIN_HIK_HCPlaySDK);
                }

                // 加载类库
                hCPlaySDK = (HCPlaySDK) Native.loadLibrary(hCPlaySDKPath, HCPlaySDK.class);
            } catch (FileNotFoundException e) {
                log.error("记载 HCPlaySDK 失败");
                e.printStackTrace();
                return null;
            }

            log.info("加载 HCPlaySDK 成功");
            return hCPlaySDK;
        }
    }

    /**
     * 配置线程池
     */
    @Bean("converterPoolExecutor")
    public ThreadPoolTaskExecutor asyncServiceExecutor(HikVisionProperties properties) {

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        // 设置核心线程数
        HikVisionProperties.HikPool pool = Optional.ofNullable(properties.getPool()).orElseGet(HikVisionProperties.HikPool::new);
        threadPoolTaskExecutor.setCorePoolSize(pool.getCorePoolSize());

        // 设置最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(pool.getMaxPoolSize());

        // 配置队列大小
        threadPoolTaskExecutor.setQueueCapacity(pool.getQueueCapacity());

        // 设置线程活跃时间（秒）
        threadPoolTaskExecutor.setKeepAliveSeconds(pool.getKeepAliveSeconds());

        // 设置默认线程名称
        threadPoolTaskExecutor.setThreadNamePrefix("kik-version");

        // 设置拒绝策略
        // CallerRunsPolicy: 不在新线程中执行任务，而是由调用者所在的线程来执行
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 执行初始化
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

}
