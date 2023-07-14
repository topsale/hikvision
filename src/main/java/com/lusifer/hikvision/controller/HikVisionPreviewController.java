package com.lusifer.hikvision.controller;

import com.lusifer.hikvision.sdk.HCUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 海康威视控制器：实时预览
 */
@RestController
public class HikVisionPreviewController {

    /**
     * 使用 RTSP 方式预览
     */
    @GetMapping(value = "/preview/rtsp.mp4", produces = {"video/x-mp4;charset=UTF-8"})
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
