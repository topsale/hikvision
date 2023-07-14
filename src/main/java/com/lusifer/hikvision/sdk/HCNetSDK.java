package com.lusifer.hikvision.sdk;

import com.sun.jna.Library;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

/**
 * 海康威视 SDK：HCNetSDK.dll
 */
public interface HCNetSDK extends Library {

    // ------------------------------------------ 回放时播放控制命令 begin ------------------------------------------

    /**
     * 关闭声音
     */
    public static final int NET_DVR_PLAY_STOP_AUDIO = 10;

    // ------------------------------------------ 结构体 begin ------------------------------------------

    /**
     * 数据时间搜索条件结构体
     */
    public static class NET_DVR_TIME_SEARCH_COND extends Structure {
        /**
         * 年，设备 OSD 时间
         */
        public short wYear;

        /**
         * 月，设备 OSD 时间
         */
        public byte byMonth;

        /**
         * 日，设备 OSD 时间
         */
        public byte byDay;

        /**
         * 时，设备 OSD 时间
         */
        public byte byHour;

        /**
         * 分，设备 OSD 时间
         */
        public byte byMinute;

        /**
         * 秒，设备 OSD 时间
         */
        public byte bySecond;

        /**
         * 0-设备本地时间，即设备 OSD 时间；
         * 1-UTC 时间
         */
        public byte byLocalOrUTC;

        /**
         * 毫秒，精度不够，默认为 0
         */
        public short wMillisecond;

        /**
         * 与 UTC 的时差（小时），-12 ... +14，+表示东区，byLocalOrUTC 为 1 时有效
         */
        public byte cTimeDifferenceH;

        /**
         * 与 UTC 的时差（分钟），-30, 0, 30, 45，+表示东区，byLocalOrUTC 为 1 时有效
         */
        public byte cTimeDifferenceM;
    }

    /**
     * 时间参数结构体
     */
    public static class NET_DVR_TIME extends Structure {
        /**
         * 年
         */
        public int dwYear;

        /**
         * 月
         */
        public int dwMonth;

        /**
         * 日
         */
        public int dwDay;

        /**
         * 时
         */
        public int dwHour;

        /**
         * 分
         */
        public int dwMinute;

        /**
         * 秒
         */
        public int dwSecond;

        /**
         * 默认格式化效果
         */
        public String toString() {
            return "NET_DVR_TIME.dwYear: " + dwYear + "\n" + "NET_DVR_TIME.dwMonth: \n" + dwMonth + "\n" + "NET_DVR_TIME.dwDay: \n" + dwDay + "\n" + "NET_DVR_TIME.dwHour: \n" + dwHour + "\n" + "NET_DVR_TIME.dwMinute: \n" + dwMinute + "\n" + "NET_DVR_TIME.dwSecond: \n" + dwSecond;
        }

        /**
         * 用于列表展示效果
         */
        public String toStringTime() {
            return String.format("%02d-%02d-%02d %02d:%02d:%02d", dwYear, dwMonth, dwDay, dwHour, dwMinute, dwSecond);
        }

        /**
         * 用于存储文件名称
         */
        public String toStringTitle() {
            return String.format("Time%02d%02d%02d%02d%02d%02d", dwYear, dwMonth, dwDay, dwHour, dwMinute, dwSecond);
        }
    }

    // ------------------------------------------ API 函数声明 begin ------------------------------------------

    /**
     * 控制录像回放的状态
     *
     * @param lPlayHandle   播放句柄
     * @param dwControlCode 控制录像回放状态命令
     * @param dwInValue     设置的参数
     * @param LPOutValue    获取的参数
     */
    boolean NET_DVR_PlayBackControl(int lPlayHandle, int dwControlCode, int dwInValue, IntByReference LPOutValue);

    /**
     * 停止回放录像文件
     * @param lPlayHandle 回放句柄
     */
    boolean NET_DVR_StopPlayBack(int lPlayHandle);
}
