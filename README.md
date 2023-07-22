# 海康威视网络摄像头

## 目录结构

```bash
    │  └─com
    │      └─lusifer
    │          └─hikvision # 项目根目录
    │              │  HikVisionApplication.java # 启动器
    │              │  HikVisionServletInitializer.java # Servlet 初始化器
    │              ├─beans
    │              │      HikVisionCamera.java # 封装摄像头基本信息
    │              ├─cache
    │              │      HikVisionCache.java # 缓存工具
    │              ├─configuration
    │              │      HikVisionAutoConfiguration.java # 摄像头 SDK 自动装配
    │              │      HikVisionProperties.java # 自定义摄像头配置
    │              ├─controller
    │              │      HikVisionCameraController.java # 摄像头控制器
    │              ├─sdk
    │              │      HCNetSDK.java # 海康威视提供的 SDK 工具类，使用 JNA 方式调用
    │              ├─thread
    │              │      CameraThread.java # 多线程调用摄像头，实现多路摄像头监控
    │              └─utils
    │                      CameraPush.java # 摄像头推流核心工具类
    │                      IpUtil.java
    │                      MD5Utils.java
    │                      OSSelectUtil.java
    │                      SpringContextHolder.java
    │                      TimerUtil.java # 定时器工具类
    └─resources
        │  application.yml # 配置文件
        ├─META-INF
        │      spring.factories
        └─sdk # 海康威视提供的 dll SDK
            └─win
                ├─ClientDemoDll
                └─HCNetSDKCom
```

## 技术架构

### 开发环境

- 语言：JDK 1.8、TypeScript
- IDE：IDEA、VSCode
- 依赖管理：Maven、npm、yarn

### 后端框架

- 基础框架：Spring Boot
- 动态库调用：JNA
- 抓流推流：JavaCV，FFmpeg（视频编码格式：H264、H265）
- 基础工具类：HuTool

### 前端框架

- Vue3
- VideoJS

### 流媒体服务器

- nginx-rtmp：http://cdn.banmajio.com/nginx.rar

## 源码说明

### 配置文件

- `application.yml`

```yarml
# 自定义海康威视配置
hik:
  # SDK 存放路径
  path: classpath:sdk/win
  # 直播流保活时间(分钟)
  keepalive: 5
  # Nginx 推送地址
  pushIp: 127.0.0.1
  # Nginx 推送端口
  pushPort: 1935
```

### Bean

- `com/lusifer/hikvision/beans/HikVisionCamera.java`

> 封装摄像头的基本信息

### Properties

- `com/lusifer/hikvision/configuration/HikVisionProperties.java`

> 封装 `application.yml` 中对于摄像头的自定义配置

- `com/lusifer/hikvision/configuration/HikVisionAutoConfiguration.java`

> 启动时通过 JNA 自动装配摄像头 SDK

### 缓存工具

- `com/lusifer/hikvision/cache/HikVisionCache.java`

> 用于保存推流信息

### 启动类

- `com/lusifer/hikvision/HikVisionApplication.java`

> 主要增加了项目结束时销毁线程池和定时器，释放资源

### 拉流、推流、转封装

- `com/lusifer/hikvision/utils/CameraPush.java`

> **拉流、推流**
>
> - 解码器：`FFmpegFrameGrabber`
> - 编码器：`FFmpegFrameRecorder`
>
> **转封装**
>
> - 优点：比转码消耗更少，降低延迟
> - 注意：转封装无法改动视频尺寸
> - 概念：视频格式(mp4，flv，avi 等)是视频编码数据封装后的结果，**转封装** 是将视频格式内的 h264,hevc 直接放入另一个格式中，所以格式发生了变化但内容没有动，因此视频尺寸数据不会发生改变；**转码** 是将格式内的 hevc 转换成 h264 再方式格式内，多了一步转换操作，自然需要消耗更多机器性能

### 定时任务

- `com/lusifer/hikvision/utils/TimerUtil.java`

> 1. 定时检查正在推流的通道使用人数，如果该通道当前使用人数为 0，则中断线程，结束该路视频推流并清除缓存
> 2. 定时检查正在推流的通道最后打开请求时间，如果与当前时间超过配置的保活时间时，则结束推流，并清除缓存

## 前端说明

### 视频播放组件

> 目前采用 VideoJS 播放摄像头直播流
>
> - 缺点：使用 VideoJS 播放 RTMP 数据使用的是 FLV 格式，需要浏览器支持 Flash，使用 **360极速浏览器(需要安装 Flash 插件)** 可解决此问题
> - 安装：
>   - `npm install vue-video-player -S`
>   - `npm install videojs-flash -S`
> - 关键代码：
>
> ```vue
> <template>
>     <div class="vedio">
>         <video id="valveVideo" class="video-js vjs-default-skin" autoplay
>             style="width: 100%;height: 100%; object-fit: fill"></video>
>     </div>
> </template>
> 
> <script setup>
> import { watch, onMounted, onUnmounted } from 'vue'
> 
> import 'video.js/dist/video-js.css'
> import videojs from 'video.js';
> import 'videojs-flash'
> 
> const options = (src) => {
>     return {
>         // 开启自动播放
>         autoplay: true,
>         // 默认情况下将会消除音频。
>         muted: true,
>         // 循环播放
>         loop: true,
>         // 关闭进度条
>         controls: false,
>         // 立即开始加载视频
>         preload: 'auto',
>         // 汉化
>         language: 'zh-CN',
>         // 按比例缩放适应容器
>         fluid: true,
>         sources: [{
>             type: 'rtmp/flv',
>             // 视频地址
>             src
>         }],
>         // 默认的错误消息
>         notSupportedMessage: '此视频暂无法播放，请稍后再试',
>         textTrackDisplay: false,
>     }
> }
> 
> // 定义播放器
> let player;
> onMounted(() => {
>     try {
>         player = videojs("valveVideo", options("rtmp://127.0.0.1:1935/live/bb40c0ad-6c26-40c2-a8e6-8e1a2b687d6e"), () => {
>             player.play();
>         });
>     } catch (error) {
>         console.log(error);
>     }
> })
> 
> onUnmounted(() => {
>     // 离开页面时销毁
>     player.dispose()
> })
> </script>
> 
> <style scoped>
> .vedio {
>     width: 632.89px;
>     height: 356px;
>     background: #000;
>     padding: 3px;
>     border: 1px solid #707070;
>     margin: 30px 30px 0 30px;
> }
> </style>
> ```

### Flash 支持

- 下载地址：https://www.flash.cn/

### 在线接口调试工具

- https://hoppscotch.io/

## 接口说明

- `com/lusifer/hikvision/controller/HikVisionCameraController.java`

### 获取服务器信息

- Path：`/status`
- Method：`GET`
- 描述：获取当前服务运行时长以及保活时长、推送IP、推送端口的信息
- 请求参数
- 返回数据

| 名称         | 类型   | 是否必须 | 默认值 | 备注             | 其他信息 |
| ------------ | ------ | -------- | ------ | ---------------- | -------- |
| uptime       | string | 必须     |        | 运行时长         |          |
| config       | object | 必须     |        | 配置参数         |          |
| ├─ keepalive | string | 必须     |        | 保活时长（分钟） |          |
| ├─ pushIp    | string | 必须     |        | 推送IP           |          |
| ├─ pushPort  | string | 必须     |        | 推送端口         |          |

### 获取视频流

- Path：`/cameras`
- Method：`GET`
- 描述：获取当前正在进行推流的设备信息
- 请求参数
- 返回数据

<table>
  <thead class="ant-table-thead">
    <tr>
      <th key=name>名称</th><th key=type>类型</th><th key=required>是否必须</th><th key=default>默认值</th><th key=desc>备注</th><th key=sub>其他信息</th>
    </tr>
  </thead><tbody className="ant-table-tbody"><tr key=0><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> </span></td><td key=1><span>object []</span></td><td key=2>非必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap"></span></td><td key=5><p key=3><span style="font-weight: '700'">item 类型: </span><span>object</span></p></td></tr><tr key=0-0><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> ip</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">设备用户名</span></td><td key=5></td></tr><tr key=0-1><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> username</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">设备密码</span></td><td key=5></td></tr><tr key=0-2><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> password</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">设备ip</span></td><td key=5></td></tr><tr key=0-3><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> channel</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">通道号</span></td><td key=5></td></tr><tr key=0-4><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> stream</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">码流(历史流不返回码流)</span></td><td key=5></td></tr><tr key=0-5><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> rtsp</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">取流地址</span></td><td key=5></td></tr><tr key=0-6><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> rtmp</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">推流地址</span></td><td key=5></td></tr><tr key=0-7><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> starttime</span></td><td key=1><span>string</span></td><td key=2>非必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">开始时间(直播流没有开始时间)</span></td><td key=5></td></tr><tr key=0-8><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> endTime</span></td><td key=1><span>string</span></td><td key=2>非必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">结束时间(直播流没有结束时间)</span></td><td key=5></td></tr><tr key=0-9><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> openTime</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">打开时间</span></td><td key=5></td></tr><tr key=0-10><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> count</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">使用人数</span></td><td key=5></td></tr><tr key=0-11><td key=0><span style="padding-left: 20px"><span style="color: #8c8a8a">├─</span> token</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">令牌</span></td><td key=5></td></tr>
               </tbody>
              </table>

### 开启视频流

- Path：`/cameras`
- Method：`POST`
- 描述：通过传入参数将 RTSP 流转为 RTMP 流进行推送。（历史流推送时，如果该设备正在推流则返回“当前视频正在使用中...”）
- 请求参数

| 参数名称     | 参数值           | 是否必须 | 示例 | 备注 |
| ------------ | ---------------- | -------- | ---- | ---- |
| Content-Type | application/json | 是       |      |      |
| **Body**     |                  |          |      |      |

<table>
  <thead class="ant-table-thead">
    <tr>
      <th key=name>名称</th><th key=type>类型</th><th key=required>是否必须</th><th key=default>默认值</th><th key=desc>备注</th><th key=sub>其他信息</th>
    </tr>
  </thead><tbody className="ant-table-tbody"><tr key=0-0><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> ip</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">设备IP</span></td><td key=5></td></tr><tr key=0-1><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> username</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">设备用户名</span></td><td key=5></td></tr><tr key=0-2><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> password</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">设备密码</span></td><td key=5></td></tr><tr key=0-3><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> channel</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">通道号</span></td><td key=5></td></tr><tr key=0-4><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> stream</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">码流(直播流需要指定码流；历史流不需要指定码流)</span></td><td key=5></td></tr><tr key=0-5><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> starttime</span></td><td key=1><span>string</span></td><td key=2>非必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">开始时间(直播流没有开始时间)</span></td><td key=5></td></tr><tr key=0-6><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> endtime</span></td><td key=1><span>string</span></td><td key=2>非必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">结束时间(直播流没有结束时间)</span></td><td key=5></td></tr>
               </tbody>
              </table>

- 返回数据

<table>
  <thead class="ant-table-thead">
    <tr>
      <th key=name>名称</th><th key=type>类型</th><th key=required>是否必须</th><th key=default>默认值</th><th key=desc>备注</th><th key=sub>其他信息</th>
    </tr>
  </thead><tbody className="ant-table-tbody"><tr key=0-0><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> token</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">令牌</span></td><td key=5></td></tr><tr key=0-1><td key=0><span style="padding-left: 0px"><span style="color: #8c8a8a"></span> uri</span></td><td key=1><span>string</span></td><td key=2>必须</td><td key=3></td><td key=4><span style="white-space: pre-wrap">推流地址</span></td><td key=5></td></tr>
               </tbody>
              </table>

### 关闭视频流

- Path：`/cameras/:tokens`
- Method：`DELETE`
- 描述：关闭正在进行的推流任务
- 请求参数

| 参数名称 | 示例 | 备注 |
| -------- | ---- | ---- |
| tokens   |      | 令牌 |

### 视频流保活

- Path：`/cameras/:tokens`
- Method：`PUT`
- 描述：对正在推送的视频流进行保活
- 请求参数

| 参数名称 | 示例 | 备注 |
| -------- | ---- | ---- |
| tokens   |      | 令牌 |

## 运行流程

- 启动 nginx-rtmp 流媒体服务器
- 启动 Spring Boot 后台
- 启动 Vue3 前端
  - 开发期间使用接口调试工具测试接口并查看效果