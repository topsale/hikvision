package com.lusifer.hikvision.utils;

import com.lusifer.hikvision.beans.HikVisionCamera;
import com.lusifer.hikvision.configuration.HikVisionProperties;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Timer;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;

/**
 * 推拉流
 */
@Slf4j
public class CameraPush {

    /**
     * 获取配置
     */
    private static HikVisionProperties properties;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        properties = applicationContext.getBean(HikVisionProperties.class);
    }

    public static Timer timer;

    protected FFmpegFrameGrabber grabber = null; // 解码器
    protected FFmpegFrameRecorder recorder = null; // 编码器
    int width; // 视频宽
    int height; // 视频高

    // 视频参数
    protected int audioCodecId;
    protected int codecId;
    protected double framerate; // 帧率
    protected int bitrate; // 比特率

    // 音频参数
    // 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
    private int audioChannels;
    private int audioBitrate;
    private int sampleRate;

    // 设备信息
    private HikVisionCamera hikVisionCamera;

    public CameraPush() {
        super();
    }

    public CameraPush(HikVisionCamera hikVisionCamera) {
        this.hikVisionCamera = hikVisionCamera;
    }

    /**
     * 选择视频源
     */
    public CameraPush from() throws Exception {
        // 采集/抓取器
        grabber = new FFmpegFrameGrabber(hikVisionCamera.getRtsp());

        // 解决 ip 输入错误时
        // grabber.start(); 出现阻塞无法释放 grabber 而导致后续推流无法进行
        Socket rtspSocket = new Socket();
        Socket rtmpSocket = new Socket();

        // 建立 TCP Socket 连接，超时时间 1s，如果成功继续执行，否则 return
        try {
            rtspSocket.connect(new InetSocketAddress(hikVisionCamera.getIp(), 554), 1000);
        } catch (IOException e) {
            grabber.stop();
            grabber.close();
            rtspSocket.close();
            log.error("与拉流地址建立连接失败...");
            return null;
        }

        try {
            rtmpSocket.connect(new InetSocketAddress(IpUtil.convert(properties.getPushIp()), Integer.parseInt(properties.getPushPort())), 1000);
        } catch (IOException e) {
            grabber.stop();
            grabber.close();
            rtspSocket.close();
            rtmpSocket.close();
            log.error("与推流地址建立连接失败...");
            return null;
        }

        if (hikVisionCamera.getRtsp().contains("rtsp")) {
            // 设置 TCP 连接
            grabber.setOption("rtsp_transport", "tcp");
        }

        // 设置采集器超时时间
        grabber.setOption("stimeout", "2000000");

        try {
            grabber.start();
            // 开始之后 ffmpeg 会采集视频信息，之后就可以获取音视频信息
            width = grabber.getImageWidth();
            height = grabber.getImageHeight();

            // 若视频像素值为 0 程序结束
            if (width == 0 && height == 0) {
                log.error("拉流超时...");
                grabber.stop();
                grabber.close();
                return null;
            }

            // 视频参数
            audioCodecId = grabber.getAudioCodec();
            log.error("音频编码：" + audioCodecId);
            codecId = grabber.getVideoCodec();
            framerate = grabber.getVideoFrameRate(); // 帧率
            bitrate = grabber.getVideoBitrate(); // 比特率

            // 音频参数
            // 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
            audioChannels = grabber.getAudioChannels();
            audioBitrate = grabber.getAudioBitrate();
            if (audioBitrate < 1) {
                audioBitrate = 128 * 1000; // 默认音频比特率
            }
        } catch (FrameGrabber.Exception e) {
            grabber.stop();
            grabber.close();
            return null;
        }

        return this;
    }

    /**
     * 选择输出
     */
    public CameraPush to() throws Exception {
        // 录制/推流器
        recorder = new FFmpegFrameRecorder(hikVisionCamera.getRtmp(), width, height);
        // 画面质量参数，0~51；18~28 是一个合理范围
        recorder.setVideoOption("crf", "28");
        recorder.setGopSize(2);
        recorder.setFrameRate(framerate);
        recorder.setVideoBitrate(bitrate);

        recorder.setAudioChannels(audioChannels);
        recorder.setAudioBitrate(audioBitrate);
        recorder.setSampleRate(sampleRate);

        AVFormatContext fc = null;
        if (hikVisionCamera.getRtmp().contains("rtmp") || hikVisionCamera.getRtmp().contains("flv")) {
            // 封装格式为 flv
            recorder.setFormat("flv");
            recorder.setAudioCodecName("aac");
            recorder.setVideoCodec(codecId);
            fc = grabber.getFormatContext();
        }
        recorder.start(fc);

        return this;
    }

    public CameraPush go(Thread thread) throws FrameGrabber.Exception, FrameRecorder.Exception {
        // 采集或推流导致的错误次数
        long errIndex = 0;

        // 连续五次没有采集到帧则认为视频采集结束，程序错误次数超过 5 次即中断程序
        for (int i = 0; i < 5 || errIndex < 5;) {
            try {
                // 用于中断线程时，结束该循环
                thread.sleep(1);
                AVPacket pkt = null;
                // 获取没有解码的音视频帧
                pkt = grabber.grabPacket();
                if (pkt == null || pkt.size() <= 0 || pkt.data() == null) {
                    // 空包记录次数跳过
                    i++;
                    errIndex++;
                    continue;
                }
                // 不需要编码直接把音视频帧推出去
                errIndex += (recorder.recordPacket(pkt) ? 0 : 1);
                av_packet_unref(pkt);
            } catch (InterruptedException e) {
                // 销毁构造器
                grabber.stop();
                grabber.close();
                recorder.stop();
                recorder.close();
                log.error("设备中断推流成功...");
                break;
            } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
                errIndex++;
            }
        }

        // 程序正常结束销毁构造器
        grabber.stop();
        grabber.close();
        recorder.stop();
        recorder.close();
        log.error("设备推流完毕...");

        return this;
    }

}
