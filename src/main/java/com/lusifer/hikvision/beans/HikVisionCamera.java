package com.lusifer.hikvision.beans;

import lombok.Data;

import java.io.Serializable;

/**
 * 海康摄像头 POJO
 */
@Data
public class HikVisionCamera implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 摄像头账号
     */
    private String username;

    /**
     * 摄像头密码
     */
    private String password;

    /**
     * 摄像头 IP
     */
    private String ip;

    /**
     * 摄像头通道
     */
    private String channel;

    /**
     * 摄像头码流
     */
    private String stream;

    /**
     * RTSP 地址
     */
    private String rtsp;

    /**
     * RTMP 地址
     */
    private String rtmp;

    /**
     * 回放开始时间
     */
    private String startTime;

    /**
     * 回放结束时间
     */
    private String endTime;

    /**
     * 打开时间
     */
    private String openTime;

    /**
     * 使用人数
     */
    private int count = 0;

    private String token;

}
