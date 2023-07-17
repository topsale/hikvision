package com.lusifer.hikvision.beans;

import lombok.Data;

import java.util.List;

/**
 * 登录配置
 */
@Data
public class HikVisionLogin {

    /**
     * 用户 ID
     */
    private int userId;

    /**
     * 序列号
     */
    private String serialNumber;

    /**
     * 通道数
     */
    private Integer channelNum;

    /**
     * 通道列表
     */
    private List<HikVisionChannel> channels;

    /**
     * 通道信息
     */
    @Data
    public static class HikVisionChannel {

        private int sdkChannelNum;

        /**
         * 通道号
         */
        private int channelNum;

        /**
         * IP
         */
        private String ip;

        /**
         * 端口
         */
        private short port;

        /**
         * 用户名
         */
        private String userName;

        /**
         * 密码
         */
        private String password;

        /**
         * 在线状态 0-在线 1-离线
         */
        private Integer onlineState;

        /**
         * 录制状态：0-不录像，1-录像
         */
        private Integer recordState;

        /**
         * 信号状态：0-正常，1-信号丢失
         */
        private Integer signalState;

        /**
         * 硬件状态: 0-正常，1-异常
         */
        private Integer hardwareStatic;

    }
}
