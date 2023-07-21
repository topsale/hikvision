package com.lusifer.hikvision;

import com.lusifer.hikvision.cache.HikVisionCache;
import com.lusifer.hikvision.thread.CameraThread;
import com.lusifer.hikvision.utils.CameraPush;
import com.lusifer.hikvision.utils.TimerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.annotation.PreDestroy;
import java.util.Date;

@Slf4j
@SpringBootApplication
public class HikVisionApplication {

    public static void main(String[] args) {
        HikVisionCache.START_TIME = new Date().getTime();
        final ApplicationContext applicationContext = SpringApplication.run(HikVisionApplication.class, args);
        // 将上下文传入 CameraPush，用于检测 TCP 连接是否正常
        CameraPush.setApplicationContext(applicationContext);
    }

    @PreDestroy
    public void destroy() {
        log.error("资源释放...");
        // 关闭线程池
        CameraThread.CameraThreadRunnable.es.shutdownNow();
        // 销毁定时器
        TimerUtil.timer.cancel();
    }

}
