package com.lusifer.hikvision.converter;

import cn.hutool.core.util.StrUtil;
import com.lusifer.hikvision.sdk.HCUtils;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import javax.servlet.AsyncContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * MP4 视频转换器
 */
@Slf4j
public class MP4Converter extends Thread implements Converter {

    private byte[] headers;
    private String rtspUrl;
    private PipedInputStream inputStream;
    private final AsyncContext context;

    /**
     * 播放 id
     */
    private Integer playHandler;

    private PipedOutputStream outputStream;

    public MP4Converter(String rtspUrl, AsyncContext context) {
        this.rtspUrl = rtspUrl;
        this.context = context;
    }

    @Override
    public void run() {
        // 采集器
        FFmpegFrameGrabber grabber = null;

        // 解码器
        FFmpegFrameRecorder recorder = null;

        // 视频流
        ByteArrayOutputStream stream = null;

        try {
            avutil.av_log_set_level(avutil.AV_LOG_ERROR);
            log.info("准备采集(RTSP): {}", rtspUrl);

            // 准备采集器
            grabber = Objects.nonNull(inputStream) ? new FFmpegFrameGrabber(inputStream, 0) : new FFmpegFrameGrabber(rtspUrl);
            if (StrUtil.isNotEmpty(rtspUrl) && rtspUrl.startsWith("rtsp")) {
                // 首选 TCP 进行 RTSP 传输
                grabber.setOption("rtsp_transport", "tcp");
                grabber.setOption("rtsp_flags", "prefer_tcp");
                log.info("请求链接(RTSP TCP)");
            }

            // 检测管道流中是否存在数据
            if (Objects.nonNull(inputStream)) {
                // 如果 2s 后依然没有写入 1024 的数据
                // 则认为管道流中无数据，避免 grabber.start(); 发生阻塞
                long stime = new Date().getTime();
                do {
                    Thread.sleep(100);
                    if (new Date().getTime() - stime > 2000) {
                        return;
                    }
                } while (inputStream.available() != 1024);
            }

            // 设置缓存大小，提高画质、减少卡顿花屏
            grabber.setOption("buffer_size", "1024000");
            grabber.startUnsafe();
            int videoCodec = grabber.getVideoCodec();
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            log.info("开始编码: {}*{}, {}", grabber.getImageWidth(), grabber.getImageHeight(), videoCodec);

            // H264 编码
            stream = new ByteArrayOutputStream();
            recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            recorder.setInterleaved(true);
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("crf", "25");
            recorder.setGopSize(50);
            recorder.setFrameRate(25);
            recorder.setSampleRate(grabber.getSampleRate());
            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioChannels(grabber.getAudioChannels());
                recorder.setAudioBitrate(grabber.getAudioBitrate());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            }
            recorder.setFormat("mp4");
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.start();
            if (headers == null) {
                headers = stream.toByteArray();
                stream.reset();
                writeResponse(headers);
            }

            // H264 解码
            while (true) {
                Frame frame = grabber.grab();
                if (frame == null) {
                    break;
                }
                recorder.record(frame);
                if (stream.size() > 0) {
                    byte[] byteArray = stream.toByteArray();
                    stream.reset();
                    try {
                        context.getResponse().getOutputStream().write(byteArray);
                    } catch (Exception e) {
                        context.complete();
                        break;
                    }
                }
                TimeUnit.MILLISECONDS.sleep(5);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (this.outputStream != null) this.outputStream.close();
                if (this.inputStream != null) this.inputStream.close();
                if (this.playHandler != null) HCUtils.stopBackPlay(this.playHandler);
                if (grabber != null) grabber.close();
                if (recorder != null) recorder.close();
                if (stream != null) stream.close();
                log.info("编码器、解码器、视频流资源已完成回收");
                context.getResponse().flushBuffer();
                context.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * MP4 转换器 通过输入流转换
     *
     * @param inputStream 输入流
     */
    public MP4Converter(PipedInputStream inputStream, PipedOutputStream outputStream, AsyncContext context, Integer playHandler) {
        this.inputStream = inputStream;
        this.context = context;
        this.outputStream = outputStream;
        this.playHandler = playHandler;
    }

    /**
     * 依次写出队列中的上下文
     *
     * @param bytes bytes
     */
    public void writeResponse(byte[] bytes) {
        try {
            context.getResponse().getOutputStream().write(bytes);
        } catch (Exception e) {
            context.complete();
        }
    }

    @Override
    public String getKey() {
        return null;
    }
}
