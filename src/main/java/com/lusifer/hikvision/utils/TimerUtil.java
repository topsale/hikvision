package com.lusifer.hikvision.utils;

import com.lusifer.hikvision.cache.HikVisionCache;
import com.lusifer.hikvision.configuration.HikVisionProperties;
import com.lusifer.hikvision.controller.HikVisionCameraController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 定时任务
 */
@Slf4j
@Component
public class TimerUtil implements CommandLineRunner {

    @Resource
    private HikVisionProperties properties;

    public static Timer timer;

    @Override
    public void run(String... args) throws Exception {
        // 超过 5 分钟，结束推流
        timer = new Timer("timeTimer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.error("开始执行定时任务...");
                // 管理缓存
                if (null != HikVisionCache.STREAM_MAP && 0 != HikVisionCache.STREAM_MAP.size()) {
                    Set<String> keys = HikVisionCache.STREAM_MAP.keySet();
                    for (String key : keys) {
                        try {
                            // 最后打开时间
                            long openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .parse(HikVisionCache.STREAM_MAP.get(key).getOpenTime()).getTime();
                            // 当前系统时间
                            long newTime = new Date().getTime();
                            // 如果通道使用人数为 0，则关闭推流
                            if (HikVisionCache.STREAM_MAP.get(key).getCount() == 0) {
                                // 结束线程
                                HikVisionCameraController.jobMap.get(key).setInterrupted();
                                // 清除缓存
                                HikVisionCache.STREAM_MAP.remove(key);
                                HikVisionCameraController.jobMap.remove(key);
                            } else if ((newTime - openTime) / 1000 / 60 > Integer.parseInt(properties.getKeepalive())) {
                                HikVisionCameraController.jobMap.get(key).setInterrupted();
                                HikVisionCameraController.jobMap.remove(key);
                                HikVisionCache.STREAM_MAP.remove(key);
                                log.error("[定时任务] 关闭" + key + " 摄像头...");
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                log.error("定时任务执行完毕...");
            }
        }, 1, 1000 * 60);
    }
}
