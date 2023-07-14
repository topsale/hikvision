package com.lusifer.hikvision.sdk;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.examples.win32.W32API;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * 海康威视 SDK：播放控制工具类
 */
public interface HCPlaySDK extends Library {

    public static final int STREAM_REALTIME = 0;

    public static final int STREAM_FILE = 1;

    boolean PlayM4_GetPort(IntByReference nPort);

    boolean PlayM4_OpenStream(int nPort, ByteByReference pFileHeadBuf, int nSize, int nBufPoolSize);

    boolean PlayM4_InputData(int nPort, ByteByReference pBuf, int nSize);

    boolean PlayM4_CloseStream(int nPort);

    boolean PlayM4_SetStreamOpenMode(int nPort, int nMode);

    boolean PlayM4_Play(int nPort, W32API.HWND hWnd);

    boolean PlayM4_Stop(int nPort);

    boolean PlayM4_SetSecretKey(int nPort, int lKeyType, String pSecretKey, int lKeyLen);

    boolean PlayM4_GetPictureSize(int nPort, IntByReference pWidth, IntByReference pHeight);

    boolean PlayM4_GetJPEG(int nPort, Pointer pBitmap, int nBufSize, IntByReference pBmpSize);

    int PlayM4_GetLastError(int nPort);

    boolean PlayM4_SetDecCallBackExMend(int nPort, DecCallBack decCBFun, Pointer pDest, int nDestSize, int nUser);

    public static interface DecCallBack extends Callback {
        void invoke(int nPort, Pointer pBuf, int nSize, FRAME_INFO pFrameInfo, int nReserved1, int nReserved2);
    }

    public class FRAME_INFO extends Structure {
        /**
         * 画面宽，单位像素。如果是音频数据，则为音频声道数
         */
        public int nWidth;

        /**
         * 画面高，单位像素。如果是音频数据，则为样位率
         */
        public int nHeight;

        /**
         * 时标信息，单位毫秒
         */
        public int nStamp;

        /**
         * 数据类型，T_AUDIO16, T_RGB32, T_YV12
         */
        public int nType;

        /**
         * 编码时产生的图像帧率，如果是音频数据则为采样率
         */
        public int nFrameRate;

        /**
         * 帧号
         */
        public int dwFrameNum;
    }

}
