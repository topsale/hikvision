package com.lusifer.hikvision.beans;

import lombok.Data;

import java.io.PipedOutputStream;

/**
 * 摄像头预览信息
 */
@Data
public class HikVisionPreview {

    /**
     * 用户id
     */
    private int userId;


    /**
     * 通道编号
     */
    private int channelNum;

    /**
     * 播放句柄
     */
    private int playHandler;

    /**
     * 播放类型： 0-实时预览，1-回放预览
     */
    private int type = 0;

    /**
     * 管道输出流
     * <p>需要对接管道输入流使用</p>
     */
    private PipedOutputStream outputStream;

    /**
     * 开始时间
     * <p>实时预览和回放都有开始时间</p>
     */
    private String beginTime;


    /**
     * 结束时间
     * <p>只有回放才有结束时间</p>
     */
    private String endTime;

}
