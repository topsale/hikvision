package com.lusifer.hikvision.controller;

import com.lusifer.hikvision.beans.HikVisionLogin;
import com.lusifer.hikvision.beans.HikVisionPreview;
import com.lusifer.hikvision.sdk.HCUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * 海康威视控制器：实时预览
 */
@RestController
public class HikVisionPreviewController {

    /**
     * 使用 SDK 方式预览
     */
    @GetMapping(value = "/video/sdk.mp4", produces = {"video/x-flv;charset=UTF-8"})
    public void flvSdkReal(HttpServletResponse response, HttpServletRequest request) {
        // 异步处理 HTTP 请求
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        // SDK 抓流，必须登录
        HikVisionLogin login = HCUtils.doLogin("192.168.1.65", "554", "admin", "Qzw121219");

        // sdk开启实时预览 （参数二为通道号，可从登陆信息获取到）
        HikVisionPreview preview = HCUtils.startRelaPlay(login.getUserId(), 1);
        assert preview != null;
        PipedOutputStream outputStream = preview.getOutputStream();
        PipedInputStream inputStream = new PipedInputStream();

        try {
            // 使用抓流器进行转码
            inputStream.connect(outputStream);
            HCUtils.streamToFlv(inputStream, outputStream, asyncContext, preview.getPlayHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用 RTSP 方式预览
     */
    @GetMapping(value = "/preview/rtsp.flv", produces = {"video/x-mp4;charset=UTF-8"})
    public void previewRTSP(HttpServletRequest request, HttpServletResponse response) {
        // 异步处理 HTTP 请求
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        // 获取 RTSP 实时流
        String rtspUrlReal = HCUtils.getRtspUrlReal("192.168.1.65", "554", "admin", "Qzw121219", 1);

        // 开始实时预览
        HCUtils.rtspToMp4(rtspUrlReal, asyncContext);
    }

}
