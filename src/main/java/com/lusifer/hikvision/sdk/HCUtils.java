package com.lusifer.hikvision.sdk;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.lusifer.hikvision.converter.MP4Converter;
import com.lusifer.hikvision.utils.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.servlet.AsyncContext;

/**
 * 海康威视工具包
 */
@Slf4j
public final class HCUtils {

    /**
     * HCNetSDK
     */
    private static final HCNetSDK hcNetSDK = SpringContextHolder.getBean(HCNetSDK.class);

    /**
     * RTSP 实时流地址
     */
    private static final String RTSP_URL_REAL = "rtsp://{}:{}@{}:{}/Streaming/Channels/{}0{}?transportmode=unicast";

    /**
     * RTSP 回访流地址
     */
    private static final String RTSP_URL_BACK = "rtsp://{}:{}@{}:{}/Streaming/tracks/{}0{}?starttime={}&endtime={}";

    /**
     * RTSP 实时流地址
     *
     * @param ip         ip
     * @param port       推流端口
     * @param username   用户名
     * @param password   密码
     * @param channelNum 通道编号
     * @return {@link String} 返回推流结果
     */
    public static String getRtspUrlReal(String ip, String port, String username, String password, int channelNum) {
        return StrUtil.format(RTSP_URL_REAL, username, password, ip, port, channelNum, 1);
    }

    /**
     * RTSP 回访流地址
     *
     * @param ip         ip
     * @param port       端口
     * @param username   用户名
     * @param password   密码
     * @param channelNum 通道编号
     * @param beginTime  开始时间 格式：yyyy-MM-dd HH:mm:ss
     * @param endTime    结束时间 格式：yyyy-MM-dd HH:mm:ss
     * @return {@link String} 返回推流结果
     */
    public static String getRtspUrlBack(String ip, String port, String username, String password, int channelNum, String beginTime, String endTime) {
        return StrUtil.format(RTSP_URL_BACK, username, password, ip, port, channelNum, 1,
                DateUtil.format(DateUtil.parseDateTime(beginTime), "yyyyMMdd't'HHmmss'z'"), DateUtil.format(DateUtil.parseDateTime(endTime), "yyyyMMdd't'HHmmss'z'"));
    }

    /**
     * 停止回放播放
     *
     * @param playHandle 播放 id
     */
    public static void stopBackPlay(int playHandle) {
        hcNetSDK.NET_DVR_PlayBackControl(playHandle, HCNetSDK.NET_DVR_PLAY_STOP_AUDIO, 0, null);
        hcNetSDK.NET_DVR_StopPlayBack(playHandle);
        log.info("停止回放: {}", playHandle);
    }

    /**
     * RTSP MP4
     */
    public static void rtspToMp4(String rtspUrl, AsyncContext context) {
        ThreadPoolTaskExecutor taskExecutor = SpringContextHolder.getBean("converterPoolExecutor");
        MP4Converter converter = new MP4Converter(rtspUrl, context);
        taskExecutor.submit(converter);
    }

}
