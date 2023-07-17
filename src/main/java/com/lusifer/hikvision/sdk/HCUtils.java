package com.lusifer.hikvision.sdk;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.lusifer.hikvision.beans.HikVisionLogin;
import com.lusifer.hikvision.beans.HikVisionPreview;
import com.lusifer.hikvision.callback.RealDataCallBack;
import com.lusifer.hikvision.converter.MP4Converter;
import com.lusifer.hikvision.utils.SpringContextHolder;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.servlet.AsyncContext;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
     * 设备登陆
     *
     * @param ip       ip
     * @param port     端口
     * @param userName 用户名
     * @param password 密码
     */
    public static HikVisionLogin doLogin(String ip, String port, String userName, String password) {
        hcNetSDK.NET_DVR_Init();
        hcNetSDK.NET_DVR_SetConnectTime(2000, 1);
        hcNetSDK.NET_DVR_SetReconnect(10000, true);

        HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG struNET_DVR_LOCAL_GENERAL_CFG = new HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG();
        // 设置 JSON 透传报警数据和图片分离
        struNET_DVR_LOCAL_GENERAL_CFG.byAlarmJsonPictureSeparate = 1;
        struNET_DVR_LOCAL_GENERAL_CFG.write();
        Pointer pStrNET_DVR_LOCAL_GENERAL_CFG = struNET_DVR_LOCAL_GENERAL_CFG.getPointer();
        hcNetSDK.NET_DVR_SetSDKLocalCfg(17, pStrNET_DVR_LOCAL_GENERAL_CFG);

        // 设备信息
        HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        int userId = hcNetSDK.NET_DVR_Login_V30(ip, Short.parseShort(port), userName, password, m_strDeviceInfo);

        // 如果注册失败返回 -1，获取错误码
        if (userId < 0) {
            log.error("设备注册失败：{}", hcNetSDK.NET_DVR_GetLastError());
            throw new RuntimeException("登陆失败");
        }

        String serialNumber = new String(m_strDeviceInfo.sSerialNumber).trim();
        log.info("设备注册成功,userId={}，设备编号：{}", userId, serialNumber);

        int maxIpChannelNum = getChannelNum(m_strDeviceInfo);
        List<HikVisionLogin.HikVisionChannel> listChannel = getChannelNumber(userId, maxIpChannelNum, m_strDeviceInfo);
        HikVisionLogin cameraInfo = new HikVisionLogin();
        cameraInfo.setChannelNum(maxIpChannelNum);
        cameraInfo.setUserId(userId);
        cameraInfo.setChannels(listChannel);
        cameraInfo.setSerialNumber(serialNumber);
        return cameraInfo;
    }

    /**
     * 获取设备的通道数量
     *
     * @param deviceInfo 设备信息
     * @return int
     */
    private static int getChannelNum(HCNetSDK.NET_DVR_DEVICEINFO_V30 deviceInfo) {
        int maxIpChannelNum;
        if (deviceInfo.byHighDChanNum == 0) {
            maxIpChannelNum = deviceInfo.byIPChanNum & 0xff;
            log.info("设备数组通道总数：{}", maxIpChannelNum);
        } else {
            maxIpChannelNum = (int) ((deviceInfo.byHighDChanNum & 0xff) << 8);
            log.info("设备数组通道总数：{}", maxIpChannelNum);
        }
        return maxIpChannelNum;
    }

    /**
     * 获取 IP 通道
     *
     * @param lUserID    用户 id 登陆成功后返回
     * @param deviceInfo 设备信息
     */
    private static List<HikVisionLogin.HikVisionChannel> getChannelNumber(int lUserID, int maxIpChannelNum, HCNetSDK.NET_DVR_DEVICEINFO_V30 deviceInfo) {
        List<HikVisionLogin.HikVisionChannel> cameraChannels = new ArrayList<>();
        // DVR 工作状态
        HCNetSDK.NET_DVR_WORKSTATE_V30 devwork = new HCNetSDK.NET_DVR_WORKSTATE_V30();
        if (!hcNetSDK.NET_DVR_GetDVRWorkState_V30(lUserID, devwork)) {
            log.info("返回设备状态失败"); // 返回Boolean值，判断是否获取设备能力
        }
        devwork.write();

        // 获取 IP 接入配置参数
        IntByReference ibrBytesReturned = new IntByReference(0);
        HCNetSDK.NET_DVR_IPPARACFG_V40 m_strIpparaCfg = new HCNetSDK.NET_DVR_IPPARACFG_V40();
        m_strIpparaCfg.write();
        //lpIpParaConfig 接收数据的缓冲指针
        Pointer lpIpParaConfig = m_strIpparaCfg.getPointer();
        boolean bRet = hcNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_IPPARACFG_V40, 0, lpIpParaConfig, m_strIpparaCfg.size(), ibrBytesReturned);
        m_strIpparaCfg.read();

        if (!bRet) {
            //设备不支持,则表示没有IP通道
            for (int chanNum = 0; chanNum < deviceInfo.byChanNum; chanNum++) {
                log.info("Camera{}", (chanNum + deviceInfo.byStartChan));
            }
            return null;
        }
        //设备支持IP通道
        for (int chanNum = 0; chanNum < maxIpChannelNum; chanNum++) {
            HCNetSDK.NET_DVR_STREAM_MODE dvrStreamMode = m_strIpparaCfg.struStreamMode[chanNum];
            dvrStreamMode.read();
            if (dvrStreamMode.byGetStreamType == 0) {
                dvrStreamMode.uGetStream.setType(HCNetSDK.NET_DVR_IPCHANINFO.class);
                dvrStreamMode.uGetStream.struChanInfo.read();
                HCNetSDK.NET_DVR_IPDEVINFO_V31 dvrIpInfo = m_strIpparaCfg.struIPDevInfo[chanNum];
                int channelID = hcNetSDK.NET_DVR_SDKChannelToISAPI(lUserID, chanNum + deviceInfo.byStartDChan, true);
                int devworkChannels = (chanNum + deviceInfo.byStartDChan - 1);

                // 设置参数
                HikVisionLogin.HikVisionChannel channel = new HikVisionLogin.HikVisionChannel();
                channel.setChannelNum(channelID);
                channel.setSdkChannelNum(chanNum + deviceInfo.byStartDChan);
                channel.setIp((new String(dvrIpInfo.struIP.sIpV4)).trim());
                channel.setPort(dvrIpInfo.wDVRPort);
                channel.setUserName(new String(dvrIpInfo.sUserName).trim());
                channel.setPassword(new String(dvrIpInfo.sPassword).trim());
                channel.setOnlineState((int) m_strIpparaCfg.struStreamMode[chanNum].uGetStream.struChanInfo.byEnable);
                channel.setRecordState((int) devwork.struChanStatic[devworkChannels].byRecordStatic);
                channel.setSignalState((int) devwork.struChanStatic[devworkChannels].bySignalStatic);
                channel.setHardwareStatic((int) devwork.struChanStatic[devworkChannels].byHardwareStatic);
                cameraChannels.add(channel);
            }
        }
        return cameraChannels;
    }

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
        hcNetSDK.NET_DVR_PlayBackControl(playHandle, HCNetSDK.NET_DVR_PLAYSTOPAUDIO, 0, null);
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

    /**
     * 实时预览视频
     *
     * @param userId     登陆返回的id
     * @param channelNum 通道编号
     * @return { @link VideoPreview } 返回预览数据
     */
    public static HikVisionPreview startRelaPlay(int userId, int channelNum) {

        HCNetSDK.NET_DVR_PREVIEWINFO strClientInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
        strClientInfo.read();
        strClientInfo.hPlayWnd = null;  //窗口句柄，从回调取流不显示一般设置为空
        strClientInfo.lChannel = channelNum;  //通道号
        strClientInfo.dwStreamType = 0; //0-主码流，1-子码流，2-三码流，3-虚拟码流，以此类推
        strClientInfo.dwLinkMode = 0; //连接方式：0- TCP方式，1- UDP方式，2- 多播方式，3- RTP方式，4- RTP/RTSP，5- RTP/HTTP，6- HRUDP（可靠传输） ，7- RTSP/HTTPS，8- NPQ
        strClientInfo.bBlocked = 1;
        strClientInfo.write();

        try {
            PipedOutputStream outputStream = new PipedOutputStream();
            RealDataCallBack realDataCallBack = SpringContextHolder.getBean(RealDataCallBack.class);
            int playHandle = hcNetSDK.NET_DVR_RealPlay_V40(userId, strClientInfo, realDataCallBack, null);
            if (playHandle == -1) {
                int iErr = hcNetSDK.NET_DVR_GetLastError();
                log.error("取流失败,错误码：{}", iErr);
                throw new RuntimeException("取流失败");
            }
            realDataCallBack.outputStreamMap.put(String.valueOf(playHandle), outputStream);
            log.info("取流成功");
            HikVisionPreview videoPreview = new HikVisionPreview();
            videoPreview.setPlayHandler(playHandle);
            videoPreview.setUserId(userId);
            videoPreview.setType(1);
            videoPreview.setChannelNum(channelNum);
            videoPreview.setOutputStream(outputStream);
            videoPreview.setBeginTime(DateUtil.formatDateTime(new Date()));
            videoPreview.setEndTime("");

            return videoPreview;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 视频码流转flv
     *
     * @param outputStream 输出流
     * @param context     上下文
     * @param playHandler 播放句柄
     */
    public static void streamToFlv(PipedInputStream inputStream, PipedOutputStream outputStream, AsyncContext context, Integer playHandler){
        ThreadPoolTaskExecutor taskExecutor = SpringContextHolder.getBean("converterPoolExecutor");
        MP4Converter converter = new MP4Converter(inputStream,outputStream,context,playHandler);
        log.info("线程池主动执行任务的线程的大致数,{}",taskExecutor.getActiveCount());
        taskExecutor.submit(converter);
    }

}
