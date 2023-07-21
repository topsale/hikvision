package com.lusifer.hikvision.controller;

import com.lusifer.hikvision.beans.HikVisionCamera;
import com.lusifer.hikvision.cache.HikVisionCache;
import com.lusifer.hikvision.configuration.HikVisionProperties;
import com.lusifer.hikvision.thread.CameraThread;
import com.lusifer.hikvision.utils.IpUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 海康威视摄像头控制器
 */
@RestController
public class HikVisionCameraController {

    @Resource
    public HikVisionProperties properties;

    public static Map<String, CameraThread.CameraThreadRunnable> jobMap = new HashMap<>();

    /**
     * 开启视频流
     *
     * @param hikVisionCamera 摄像头参数
     * @return Map<String, String>
     */
    @PostMapping(value = "/cameras")
    public Map<String, String> openCamera(@RequestBody HikVisionCamera hikVisionCamera) {
        // 返回结果
        Map<String, String> map = new HashMap<>();
        // 校验参数
        if (null != hikVisionCamera.getIp() && !"".equals(hikVisionCamera.getIp())
                && null != hikVisionCamera.getUsername() && !"".equals(hikVisionCamera.getUsername())
                && null != hikVisionCamera.getPassword() && !"".equals(hikVisionCamera.getPassword())
                && null != hikVisionCamera.getChannel()
                && !"".equals(hikVisionCamera.getChannel())) {
            HikVisionCamera camera = new HikVisionCamera();
            // 获取当前时间
            String openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());
            Set<String> keys = HikVisionCache.STREAM_MAP.keySet();
            // 缓存是否为空
            if (0 == keys.size()) {
                // 开始推流
                camera = openStream(hikVisionCamera.getIp(), hikVisionCamera.getUsername(), hikVisionCamera.getPassword(), hikVisionCamera.getChannel(),
                        hikVisionCamera.getStream(), hikVisionCamera.getStartTime(), hikVisionCamera.getEndTime(), openTime);
                map.put("token", camera.getToken());
                map.put("url", camera.getRtmp());
            } else {
                // 是否存在的标志；0：不存在；1：存在
                int sign = 0;
                // 直播流
                if (null == hikVisionCamera.getStartTime()) {
                    for (String key : keys) {
                        // 存在直播流
                        if (hikVisionCamera.getIp().equals(HikVisionCache.STREAM_MAP.get(key).getIp())
                                && hikVisionCamera.getChannel().equals(HikVisionCache.STREAM_MAP.get(key).getChannel())
                                && null == HikVisionCache.STREAM_MAP.get(key).getStartTime()) {
                            camera = HikVisionCache.STREAM_MAP.get(key);
                            sign = 1;
                            break;
                        }
                    }

                    // 存在
                    if (sign == 1) {
                        camera.setCount(camera.getCount() + 1);
                        camera.setOpenTime(openTime);
                        map.put("token", camera.getToken());
                        map.put("url", camera.getRtmp());
                    } else {
                        camera = openStream(hikVisionCamera.getIp(), hikVisionCamera.getUsername(), hikVisionCamera.getPassword(), hikVisionCamera.getChannel(),
                                hikVisionCamera.getStream(), hikVisionCamera.getStartTime(), hikVisionCamera.getEndTime(), openTime);
                        map.put("token", camera.getToken());
                        map.put("url", camera.getRtmp());
                    }

                } else {// 历史流
                    for (String key : keys) {
                        if (hikVisionCamera.getIp().equals(HikVisionCache.STREAM_MAP.get(key).getIp())
                                && hikVisionCamera.getChannel().equals(HikVisionCache.STREAM_MAP.get(key).getChannel())
                                && null != HikVisionCache.STREAM_MAP.get(key).getStartTime()) {// 存在历史流
                            camera = HikVisionCache.STREAM_MAP.get(key);
                            sign = 1;
                            break;
                        }
                    }
                    if (sign == 1) {
                        camera.setCount(camera.getCount() + 1);
                        camera.setOpenTime(openTime);
                        map.put("message", "当前视频正在使用中...");
                    } else {
                        camera = openStream(hikVisionCamera.getIp(), hikVisionCamera.getUsername(), hikVisionCamera.getPassword(), hikVisionCamera.getChannel(),
                                hikVisionCamera.getStream(), hikVisionCamera.getStartTime(), hikVisionCamera.getEndTime(), openTime);
                        map.put("token", camera.getToken());
                        map.put("url", camera.getRtmp());
                    }
                }
            }
        }

        return map;
    }

    /**
     * 关闭视频流
     */
    @DeleteMapping(value = "/cameras/{tokens}")
    public void closeCamera(@PathVariable("tokens") String tokens) {
        if (null != tokens && !"".equals(tokens)) {
            String[] tokenArr = tokens.split(",");
            for (String token : tokenArr) {
                if (jobMap.containsKey(token) && HikVisionCache.STREAM_MAP.containsKey(token)) {
                    if (0 < HikVisionCache.STREAM_MAP.get(token).getCount()) {
                        // 人数 -1
                        HikVisionCache.STREAM_MAP.get(token).setCount(HikVisionCache.STREAM_MAP.get(token).getCount() - 1);
                    }
                }
            }
        }
    }

    /**
     * 获取视频流
     *
     * @return Map<String, HikVisionCamera>
     */
    @GetMapping(value = "/cameras")
    public Map<String, HikVisionCamera> getCameras() {
        return HikVisionCache.STREAM_MAP;
    }

    /**
     * 视频流保活
     */
    @PutMapping(value = "/cameras/{tokens}")
    public void keepAlive(@PathVariable("tokens") String tokens) {
        // 校验参数
        if (null != tokens && !"".equals(tokens)) {
            String[] tokenArr = tokens.split(",");
            for (String token : tokenArr) {
                HikVisionCamera hikVisionCamera = new HikVisionCamera();
                // 直播流token
                if (null != HikVisionCache.STREAM_MAP.get(token)) {
                    hikVisionCamera = HikVisionCache.STREAM_MAP.get(token);
                    // 更新当前系统时间
                    hikVisionCamera.setOpenTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
                }
            }
        }
    }

    /**
     * 获取服务器信息
     *
     * @return Map<String, Object>
     */
    @GetMapping(value = "/status")
    public Map<String, Object> getConfig() {
        // 获取当前时间
        long nowTime = new Date().getTime();
        String upTime = (nowTime - HikVisionCache.START_TIME) / (1000 * 60 * 60) + "h"
                + (nowTime - HikVisionCache.START_TIME) % (1000 * 60 * 60) / (1000 * 60) + "m"
                + (nowTime - HikVisionCache.START_TIME) % (1000 * 60 * 60) / (1000) + "s";
        Map<String, Object> status = new HashMap<>();
        status.put("config", properties);
        status.put("uptime", upTime);
        return status;
    }

    /**
     * 推流器
     *
     * @param ip        摄像头 IP
     * @param username  账号
     * @param password  密码
     * @param channel   通道
     * @param stream    码流
     * @param starttime 开始时间
     * @param endtime   结束时间
     * @param openTime  打开时间
     * @return HikVisionCamera
     */
    private HikVisionCamera openStream(String ip, String username, String password, String channel, String stream, String starttime, String endtime, String openTime) {
        HikVisionCamera hikVisionCamera = new HikVisionCamera();

        // 生成 token
        String token = UUID.randomUUID().toString();
        String rtsp = "";
        String rtmp = "";
        String domain = IpUtil.convert(ip);

        // 历史流
        if (null != starttime && !"".equals(starttime)) {
            if (null != endtime && !"".equals(endtime)) {
                rtsp = "rtsp://" + username + ":" + password + "@" + domain + ":554/Streaming/tracks/" + channel
                        + "01?starttime=" + starttime.substring(0, 8) + "t" + starttime.substring(8) + "z'&'endtime="
                        + endtime.substring(0, 8) + "t" + endtime.substring(8) + "z";
                hikVisionCamera.setStartTime(starttime);
                hikVisionCamera.setEndTime(endtime);
            } else {
                try {
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                    String startTime = df.format(df.parse(starttime).getTime() - 60 * 1000);
                    String endTime = df.format(df.parse(starttime).getTime() + 60 * 1000);
                    rtsp = "rtsp://" + username + ":" + password + "@" + domain + ":554/Streaming/tracks/" + channel
                            + "01?starttime=" + startTime.substring(0, 8) + "t" + startTime.substring(8)
                            + "z'&'endtime=" + endTime.substring(0, 8) + "t" + endTime.substring(8) + "z";
                    hikVisionCamera.setStartTime(startTime);
                    hikVisionCamera.setEndTime(endTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            rtmp = "rtmp://" + IpUtil.convert(properties.getPushIp()) + ":" + properties.getPushPort() + "/history/"
                    + token;
        }
        // 直播流
        else {
            rtsp = "rtsp://" + username + ":" + password + "@" + domain + ":554/h264/ch" + channel + "/" + stream
                    + "/av_stream";
            rtmp = "rtmp://" + IpUtil.convert(properties.getPushIp()) + ":" + properties.getPushPort() + "/live/" + token;
        }

        hikVisionCamera.setUsername(username);
        hikVisionCamera.setPassword(password);
        hikVisionCamera.setIp(ip);
        hikVisionCamera.setChannel(channel);
        hikVisionCamera.setStream(stream);
        hikVisionCamera.setRtsp(rtsp);
        hikVisionCamera.setRtmp(rtmp);
        hikVisionCamera.setOpenTime(openTime);
        hikVisionCamera.setCount(1);
        hikVisionCamera.setToken(token);

        // 执行任务
        CameraThread.CameraThreadRunnable job = new CameraThread.CameraThreadRunnable(hikVisionCamera);
        CameraThread.CameraThreadRunnable.es.execute(job);
        jobMap.put(token, job);

        return hikVisionCamera;
    }
}
