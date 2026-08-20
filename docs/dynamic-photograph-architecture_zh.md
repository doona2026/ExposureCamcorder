# Dynamic Photograph 实现设计文档

## 1. 文档目标

本文档将当前的 Dynamic Photograph 方案整理为可直接开发的实现蓝图，重点覆盖：

- 包结构
- 类清单
- 每个类的职责
- 数据组件与网络协议
- 录制会话状态机
- 与当前 `ExposureCamcorder` 工程的衔接建议

本文档默认沿用当前项目的包根：

`io.github.exposure_camcorder`

虽然功能方向已经从“录像机”收缩为“动态照片”，第一版建议先不改 `mod id` 和顶级包名，避免过早进行工程级重命名。

## 2. 第一版产品边界

Dynamic Photograph 是 `Exposure` 生态中的一种新照片，不是真正的视频系统。

第一版范围：

- 玩家手持相机
- 必须装入专用动态胶卷
- 必须主动切换到 Dynamic 模式
- 按住快门持续采样
- 采样间隔可配置，默认 `2 tick/帧`
- 最长录制时长默认 `4 秒`，可配置
- 每帧继续复用 Exposure 现有单帧存储链路
- 成品为 `DynamicPhotographItem`
- 成品内部保存 `List<Frame> + 播放设置 + sessionId`
- 查看界面打开后自动播放，可手动暂停
- 非查看界面只显示封面静帧

第一版明确不做：

- 音频
- GIF/视频导出
- 相框动画播放
- 帧编辑、删帧、重排
- 普通照片复制/堆叠/冲洗配方兼容
- 多段动态胶卷

## 3. 总体架构

### 3.1 设计原则

- 服务端管理会话合法性、时长上限、帧上限和结束条件
- 客户端负责实际采样与上传图像数据
- 服务端为每一帧创建 `Frame`
- 动态照片成品只在录制结束时生成
- 录制中的临时状态不写入物品，而保存在玩家活动会话中
- 每帧图像仍进入 Exposure 的 `ExposureData` 外部存储，物品内部不直接保存像素

### 3.2 核心数据流

1. 玩家装入动态胶卷并切换到 Dynamic 模式
2. 玩家按下快门
3. 服务端创建 `DynamicCaptureSession`
4. 服务端按录制参数驱动逐帧采集
5. 每个采样点：
   - 服务端创建一个新的 `Frame`
   - 服务端向客户端发送本帧采集请求
   - 客户端抓取图像并上传 `ExposureData`
   - 服务端落库该帧 exposure
   - 服务端将 `Frame` 追加到会话
6. 玩家松手、超时、胶卷耗尽或被打断时结束会话
7. 若帧数大于等于 1，封装 `DynamicPhotographItem`
8. 玩家打开动态照片时进入查看界面自动播放

## 4. 推荐包结构

建议在现有 `common` 模块中按下面结构组织：

```text
io.github.exposure_camcorder
├─ Config
├─ ExposureCamcorder
├─ ExposureCamcorderClient
├─ PlatformHelper
├─ Register
├─ client
│  ├─ capture
│  │  ├─ DynamicFrameCaptureClient
│  │  └─ DynamicFrameUploadQueue
│  ├─ gui
│  │  ├─ component
│  │  │  ├─ DynamicPlaybackControls
│  │  │  └─ DynamicRecordingStatusOverlay
│  │  └─ screen
│  │     ├─ DynamicPhotographScreenController
│  │     └─ DynamicPhotographViewModel
│  ├─ playback
│  │  ├─ DynamicPlaybackController
│  │  └─ DynamicPlaybackSession
│  └─ render
│     ├─ DynamicPhotographCoverResolver
│     └─ DynamicPhotographFrameResolver
├─ compatibility
│  ├─ exposure
│  │  ├─ ExposureAccess
│  │  ├─ ExposureCameraHooks
│  │  └─ ExposurePhotographScreenHooks
│  └─ mixin
│     ├─ client
│     └─ common
├─ network
│  ├─ packet
│  │  ├─ c2s
│  │  │  ├─ DynamicCaptureFrameDataC2SP
│  │  │  ├─ DynamicCaptureStopC2SP
│  │  │  └─ DynamicPlaybackQueryC2SP
│  │  └─ s2c
│  │     ├─ DynamicCaptureStartS2CP
│  │     ├─ DynamicCaptureFrameRequestS2CP
│  │     ├─ DynamicCaptureStateS2CP
│  │     └─ DynamicPhotographDataS2CP
│  ├─ C2SPackets
│  ├─ CommonPackets
│  └─ S2CPackets
├─ world
│  ├─ component
│  │  ├─ DynamicPhotographFrames
│  │  ├─ DynamicPhotographSettings
│  │  ├─ DynamicPhotographSummary
│  │  └─ DynamicSessionId
│  ├─ item
│  │  ├─ DynamicFilmItem
│  │  ├─ DynamicPhotographItem
│  │  ├─ DynamicMode
│  │  └─ camera
│  │     ├─ DynamicCameraModeController
│  │     ├─ DynamicCameraModeState
│  │     └─ DynamicRecordingTrigger
│  └─ session
│     ├─ DynamicCaptureSession
│     ├─ DynamicCaptureSessionEndReason
│     ├─ DynamicCaptureSessionManager
│     ├─ DynamicCaptureSessionResult
│     └─ DynamicCaptureTicker
└─ util
   ├─ DynamicPhotographFactory
   ├─ DynamicPhotographTiming
   └─ DynamicPhotographValidation
```

## 5. 类清单与职责表

### 5.1 核心物品与数据组件

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `DynamicFilmItem` | `world.item` | Item | 专用动态胶卷。定义最大帧数、默认时长、tooltip、容量展示、一次拍摄一卷的消耗规则。 |
| `DynamicPhotographItem` | `world.item` | Item | 动态照片成品。负责 use 行为、tooltip、封面读取、与查看界面的打开逻辑。 |
| `DynamicMode` | `world.item` | enum/record | 描述相机当前是否处于 Dynamic 模式，以及相关参数入口。 |
| `DynamicPhotographFrames` | `world.component` | component payload | 封装 `List<Frame>`，避免在业务代码中直接裸用列表。 |
| `DynamicPhotographSettings` | `world.component` | record | 保存 `captureIntervalTicks`、`defaultPlaybackTicksPerFrame`、`loop`、`coverFrameIndex`。 |
| `DynamicPhotographSummary` | `world.component` | record | 供 tooltip 和静态展示使用的摘要信息，例如帧数、估算时长、封面索引。 |
| `DynamicSessionId` | `world.component` | record/value | 保存动态照片所属的 `sessionId`，用于追踪、调试和未来扩展。 |

### 5.2 录制会话与服务端状态机

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `DynamicCaptureSession` | `world.session` | record/class | 单次动态拍摄会话对象。保存 `sessionId`、玩家、起始 tick、采样间隔、最大帧数、当前帧列表和结束状态。 |
| `DynamicCaptureSessionManager` | `world.session` | manager | 服务端会话总管。负责开始会话、获取玩家活动会话、结束会话、会话合法性校验。 |
| `DynamicCaptureTicker` | `world.session` | tick service | 在服务端 tick 中推进活动会话，决定何时请求下一帧采集。 |
| `DynamicCaptureSessionEndReason` | `world.session` | enum | 会话结束原因，例如 `RELEASED`、`TIME_LIMIT`、`FILM_EXHAUSTED`、`INTERRUPTED`、`PLAYER_LEFT`。 |
| `DynamicCaptureSessionResult` | `world.session` | record | 会话结束后的结构化结果，供物品生成和提示逻辑使用。 |

### 5.3 相机模式与交互控制

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `DynamicCameraModeController` | `world.item.camera` | service | 把 Dynamic 模式挂接到 Exposure 相机逻辑中，处理模式切换、参数读取和录制入口判断。 |
| `DynamicCameraModeState` | `world.item.camera` | record | 代表相机当前 Dynamic 模式参数，如采样间隔、默认播放速度、是否装入动态胶卷。 |
| `DynamicRecordingTrigger` | `world.item.camera` | service | 处理按下快门、持续按住、松开快门的录制触发语义。 |

### 5.4 网络协议

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `DynamicCaptureStartS2CP` | `network.packet.s2c` | packet | 通知客户端开始一段动态录制会话，并下发必要的会话参数。 |
| `DynamicCaptureFrameRequestS2CP` | `network.packet.s2c` | packet | 请求客户端采集某一帧图像。应携带 `sessionId`、`frameIndex`、`exposureId` 和采集参数。 |
| `DynamicCaptureStateS2CP` | `network.packet.s2c` | packet | 更新客户端录制状态 UI，例如已录帧数、剩余容量、剩余时长。 |
| `DynamicPhotographDataS2CP` | `network.packet.s2c` | packet | 查看界面需要时下发动态照片的可播放数据摘要或缺失状态。 |
| `DynamicCaptureFrameDataC2SP` | `network.packet.c2s` | packet | 客户端将本帧的 `ExposureData` 上传给服务端。 |
| `DynamicCaptureStopC2SP` | `network.packet.c2s` | packet | 客户端主动请求停止录制，例如松手事件同步。 |
| `DynamicPlaybackQueryC2SP` | `network.packet.c2s` | packet | 客户端查看动态照片时请求额外播放数据。 |

### 5.5 客户端采样与上传

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `DynamicFrameCaptureClient` | `client.capture` | client service | 收到逐帧请求后执行真正的画面采样，构造 `ExposureData` 并交给上传队列。 |
| `DynamicFrameUploadQueue` | `client.capture` | client queue | 管理逐帧上传节奏，避免同一 tick 内重复提交或乱序提交。 |
| `DynamicRecordingStatusOverlay` | `client.gui.component` | UI component | 在相机 UI 或取景器上显示 `REC`、已录帧数、剩余时长、剩余容量。 |

### 5.6 查看界面与播放

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `DynamicPhotographScreenController` | `client.gui.screen` | controller | 挂接到现有 `PhotographScreen` 的动态播放行为控制层。负责把动态与静态逻辑解耦。 |
| `DynamicPhotographViewModel` | `client.gui.screen` | view model | 为界面提供当前帧、播放速度、暂停状态、封面回退结果等视图状态。 |
| `DynamicPlaybackController` | `client.playback` | service | 控制自动播放、暂停、恢复、循环、当前帧推进。 |
| `DynamicPlaybackSession` | `client.playback` | record/class | 单次查看会话状态，包含当前帧索引、当前会话播放速度和是否暂停。 |
| `DynamicPlaybackControls` | `client.gui.component` | UI component | 播放/暂停按钮和速度调整控件。第一版速度调整只影响当前查看会话。 |
| `DynamicPhotographCoverResolver` | `client.render` | service | 非查看界面解析应显示哪一帧。先用封面帧，缺失时回退下一可用帧。 |
| `DynamicPhotographFrameResolver` | `client.render` | service | 查看界面按需解析当前应显示的帧数据，并处理缺帧容错。 |

### 5.7 兼容层与工具类

| 类名 | 包 | 类型 | 职责 |
|---|---|---|---|
| `ExposureAccess` | `compatibility.exposure` | facade | 封装对 Exposure 内部类型、数据组件和存储仓库的访问。 |
| `ExposureCameraHooks` | `compatibility.exposure` | hook service | 把动态模式接入 Exposure 相机行为。 |
| `ExposurePhotographScreenHooks` | `compatibility.exposure` | hook service | 把动态照片查看逻辑接入现有 `PhotographScreen`。 |
| `DynamicPhotographFactory` | `util` | factory | 会话结束时根据 `Frame` 列表和设置生成最终 `DynamicPhotographItem`。 |
| `DynamicPhotographTiming` | `util` | utility | 统一处理帧数、tick、秒数和播放速度换算。 |
| `DynamicPhotographValidation` | `util` | utility | 校验动态胶卷、会话参数、帧列表合法性和结束生成条件。 |

## 6. 建议保留、替换与废弃的当前类

当前工程已有明显的“录像机”实现痕迹。建议如下：

| 当前类 | 建议 | 说明 |
|---|---|---|
| `world.item.CamcorderItem` | 替换/重构 | 如果继续保留“相机形态”，建议逐步重构为动态照片相机入口，而不是继续沿用视频录制语义。 |
| `world.item.VideoTapeItem` | 替换 | 改为 `DynamicFilmItem`，语义从“录像带”收缩为“动态胶卷”。 |
| `world.storage.Recording` | 替换 | 改成 `DynamicCaptureSession` 或仅保留部分字段思想，不再使用视频术语。 |
| `world.storage.RecordingSavedData` | 大概率废弃 | 第一版不需要把活动录制会话做成长期世界存档；成品仍依赖 Exposure 的单帧存储。 |
| `client.gui.screen.PlaybackScreen` | 重构 | 可吸收部分播放 UI 经验，但目标应转为动态照片查看，而不是录像带播放器。 |
| `client.VideoFrameCapture` | 部分保留 | 可以保留采样实现思路，但要改为按动态拍摄协议服务，而不是长录像逻辑。 |
| `client.RecordingCache` | 重构或废弃 | 若职责是长视频缓存，应拆成动态照片查看缓存或帧上传队列。 |
| `world.block.ProjectorBlock` / `ProjectorBlockEntity` | 暂缓 | 第一版边界里不做投影和世界内动画播放，可以先不接动态照片。 |

## 7. 数据组件设计

建议注册以下数据组件：

| 组件名 | 数据类型 | 用途 |
|---|---|---|
| `DYNAMIC_PHOTOGRAPH_FRAMES` | `List<Frame>` | 动态照片内部保存的多帧引用列表。 |
| `DYNAMIC_PHOTOGRAPH_SETTINGS` | `DynamicPhotographSettings` | 保存采样间隔、默认播放速度、循环和封面索引。 |
| `DYNAMIC_PHOTOGRAPH_SUMMARY` | `DynamicPhotographSummary` | 为 tooltip 和简单展示提供摘要。 |
| `DYNAMIC_PHOTOGRAPH_SESSION_ID` | `String` 或 record | 追踪来源会话。 |
| `DYNAMIC_CAMERA_MODE_STATE` | `DynamicCameraModeState` | 保存相机 Dynamic 模式参数。 |

建议 `DynamicPhotographSettings` 第一版字段为：

| 字段 | 类型 | 说明 |
|---|---|---|
| `captureIntervalTicks` | `int` | 录制采样间隔，默认 `2` |
| `defaultPlaybackTicksPerFrame` | `int` | 默认播放速度，初始与录制一致 |
| `loop` | `boolean` | 默认是否循环，第一版默认 `true` |
| `coverFrameIndex` | `int` | 默认封面帧索引，第一版默认 `0` |

## 8. 网络协议设计

### 8.1 服务端到客户端

| 包名 | 触发时机 | 最小内容 |
|---|---|---|
| `DynamicCaptureStartS2CP` | 开始录制 | `sessionId`、`captureIntervalTicks`、`maxFrames`、UI 展示所需初始状态 |
| `DynamicCaptureFrameRequestS2CP` | 请求某一帧 | `sessionId`、`frameIndex`、`exposureId`、采集参数 |
| `DynamicCaptureStateS2CP` | 状态更新 | `sessionId`、已录帧数、剩余帧数、剩余估算时长 |
| `DynamicPhotographDataS2CP` | 查看界面请求返回 | 帧列表摘要、缺失帧状态、可播放元信息 |

### 8.2 客户端到服务端

| 包名 | 触发时机 | 最小内容 |
|---|---|---|
| `DynamicCaptureFrameDataC2SP` | 完成某一帧采样后 | `sessionId`、`frameIndex`、`exposureId`、`ExposureData` |
| `DynamicCaptureStopC2SP` | 玩家主动松手 | `sessionId`、停止原因 |
| `DynamicPlaybackQueryC2SP` | 打开查看界面时 | 动态照片物品标识或会话标识 |

## 9. 录制会话状态机

### 9.1 会话状态

建议 `DynamicCaptureSession` 采用以下状态流：

```text
IDLE
  -> STARTING
  -> RECORDING
  -> STOPPING
  -> FINISHED
```

### 9.2 结束原因

建议统一使用以下结束原因：

- `RELEASED`
- `TIME_LIMIT`
- `FILM_EXHAUSTED`
- `INTERRUPTED`
- `PLAYER_LEFT`
- `PLAYER_DIED`
- `DIMENSION_CHANGED`
- `INVALIDATED`

### 9.3 收尾规则

| 条件 | 结果 |
|---|---|
| `0 帧` | 不生成物品，提示失败 |
| `1 帧` | 仍生成 `DynamicPhotographItem` |
| `>= 1 帧` 且背包有空间 | 放入背包 |
| `>= 1 帧` 且背包已满 | 掉落玩家脚边并提示 |

## 10. 查看与渲染规则

### 10.1 非查看界面

- 物品栏、tooltip、相框、普通展示场景一律只显示封面静帧
- 封面缺失时，回退到下一可用帧
- 全部帧缺失时显示损坏占位效果

### 10.2 查看界面

- 打开后自动播放
- 支持手动暂停
- 暂停时停在当前帧
- 默认循环播放
- 允许调整播放速度
- 第一版播放速度只影响当前查看会话，不回写物品
- 第一版不支持删帧、重排、导出或保存新版本

## 11. 推荐实现顺序

### 阶段 1：数据模型与物品

1. 注册 `DynamicFilmItem`
2. 注册 `DynamicPhotographItem`
3. 注册动态照片相关数据组件
4. 实现基础 tooltip 与封面展示

### 阶段 2：服务端会话主链

1. 实现 `DynamicCaptureSession`
2. 实现 `DynamicCaptureSessionManager`
3. 实现 `DynamicCaptureTicker`
4. 实现会话结束与物品封装规则

### 阶段 3：逐帧采样协议

1. 增加动态录制专用 packet
2. 客户端实现逐帧采样与上传
3. 服务端把每帧写入 Exposure 存储并追加到会话

### 阶段 4：相机模式接入

1. Dynamic 模式切换
2. 动态胶卷校验
3. 按下、按住、松手触发语义
4. 录制中 UI 状态提示

### 阶段 5：查看界面

1. 抽离动态播放控制器
2. 接入现有照片查看界面
3. 自动播放、暂停、调速
4. 缺帧容错

## 12. 最小可行版本验收标准

完成以下能力即可视为第一版闭环达成：

- 能装入动态胶卷并切换到 Dynamic 模式
- 能按住快门录制并按配置间隔逐帧采样
- 能在松手、超时、耗尽或打断时正确收尾
- 能生成 `DynamicPhotographItem`
- 能在 tooltip 中显示帧数、间隔和估算时长
- 能打开查看界面自动播放
- 能手动暂停和调节当前查看会话播放速度
- 非查看界面只显示封面静帧

## 13. 当前项目的落地建议

结合当前 `ExposureCamcorder` 工程，建议近期的重构方向是：

1. 保留包根 `io.github.exposure_camcorder`
2. 新增 `docs/`，把产品方向正式改写为 Dynamic Photograph
3. 优先新增新类，不要一上来大面积改旧类
4. 等第一版跑通后，再决定是否把旧的 `Camcorder` / `VideoTape` 术语统一替换

这样可以最大化减少一次性重命名和大手术带来的风险。
