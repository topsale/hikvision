package com.lusifer.hikvision.thread;

import com.lusifer.hikvision.beans.HikVisionCamera;
import com.lusifer.hikvision.cache.HikVisionCache;
import com.lusifer.hikvision.controller.HikVisionCameraController;
import com.lusifer.hikvision.utils.CameraPush;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 摄像头线程
 */
@Slf4j
public class CameraThread {

    public static class CameraThreadRunnable implements Runnable {

        /**
         * 创建线程池
         */
        public static ExecutorService es = Executors.newCachedThreadPool();

        private final HikVisionCamera hikVisionCamera;
        private Thread thread;

        public CameraThreadRunnable(HikVisionCamera hikVisionCamera) {
            this.hikVisionCamera = hikVisionCamera;
        }

        /**
         * 中断线程
         */
        public void setInterrupted() {
            thread.interrupt();
        }

        @Override
        public void run() {
            // 直播流
            try {
                // 获取当前线程存入缓存
                thread = Thread.currentThread();
                HikVisionCache.STREAM_MAP.put(hikVisionCamera.getToken(), hikVisionCamera);
                // 执行转流推流任务
                CameraPush push = new CameraPush(hikVisionCamera).from();
                if (push != null) {
                    push.to().go(thread);
                }

                // 清除缓存
                HikVisionCache.STREAM_MAP.remove(hikVisionCamera.getToken());
                HikVisionCameraController.jobMap.remove(hikVisionCamera.getToken());
            } catch (Exception e) {
                log.error("当前线程：" + Thread.currentThread().getName() + " 当前任务：" + hikVisionCamera.getRtsp() + " 停止...");
                HikVisionCache.STREAM_MAP.remove(hikVisionCamera.getToken());
                HikVisionCameraController.jobMap.remove(hikVisionCamera.getToken());
                e.printStackTrace();
            }
        }
    }

}
