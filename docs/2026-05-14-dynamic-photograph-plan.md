# Dynamic Photograph 实现计划

> 日期: 2026-05-14
> 基于设计文档: `docs/superpowers/specs/2026-05-14-dynamic-photograph-design.md`
> 设计正文: `mod/ExposureCamcorder/docs/dynamic-photograph-architecture_zh.md`
> 项目目录: `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\`
> 预计工时: `2.5 - 4.5` 天
> 执行方式: 先打通最小闭环，再处理旧录像路径的收缩和隔离

---

## 计划目标

将当前 `ExposureCamcorder` 从“录像机”原型收缩为 `Exposure` 生态下的 `Dynamic Photograph` 附属模组，第一版完成以下闭环：

- 装入动态胶卷并切到 Dynamic 模式
- 按住快门持续采样
- 服务端管理录制会话并逐帧落库到 Exposure
- 录制结束后生成 `DynamicPhotographItem`
- 动态照片在查看界面自动播放
- 非查看界面只显示封面静帧

## 约束

- 不做音频
- 不做 GIF / 视频导出
- 不做世界内相框动画
- 不做帧编辑
- 不做普通照片复制/堆叠兼容
- 不做多段动态胶卷

## 执行顺序

按 8 个阶段执行，每个阶段内任务保持原子化，并在完成后立刻运行对应验证命令。

---

## Phase 0：基线、测试脚手架与入口整理

目标：让当前工程具备可测试、可迭代的改造基础，并明确新方案的启动入口。

### 任务 0.1：补齐 `common` 测试基础设施

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\build.gradle`

**具体修改**

- 添加 `testImplementation` 的 JUnit 5 依赖
- 配置 `test` 任务使用 JUnit Platform
- 保持现有 Architectury 结构不变

**预期结果**

- `common` 模块可以承载纯 Java/逻辑层测试

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:test
```

---

### 任务 0.2：创建测试源码目录与基础测试包

**新增路径**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\util\`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\world\session\`

**具体修改**

- 创建测试目录
- 放入空的包占位测试类，确保后续每个逻辑阶段都能直接补测试

**预期结果**

- 后续工具类与会话状态机可以按 Red-Green-Refactor 方式推进

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:test
```

---

### 任务 0.3：整理主入口职责

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\ExposureCamcorder.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\ExposureCamcorderClient.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\Register.java`

**具体修改**

- 把初始化拆分为更清晰的注册顺序：
  - 数据组件
  - 物品
  - 网络包
  - 客户端播放/覆盖层
- 保留现有 `mod id` 和包根，不做大重命名

**预期结果**

- 后续新增类不需要继续把逻辑堆到旧录像入口里

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

## Phase 1：数据模型与配置

目标：先把 Dynamic Photograph 的核心数据结构稳定下来。

### 任务 1.1：实现动态照片设置与摘要组件

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\component\DynamicPhotographSettings.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\component\DynamicPhotographSummary.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\component\DynamicSessionId.java`

**具体修改**

- 定义 codec / stream codec
- 覆盖第一版字段：
  - `captureIntervalTicks`
  - `defaultPlaybackTicksPerFrame`
  - `loop`
  - `coverFrameIndex`
- `DynamicPhotographSummary` 包含帧数、估算时长、封面索引

**预期结果**

- 动态照片的持久化元数据具备稳定结构

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 1.2：实现帧列表组件包装类

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\component\DynamicPhotographFrames.java`

**具体修改**

- 包装 `List<Frame>`
- 提供只读访问、帧数查询、封面索引访问辅助方法

**预期结果**

- 业务代码不再直接裸用 `List<Frame>`

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 1.3：注册新的数据组件

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\Register.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\ExposureCamcorder.java`

**具体修改**

- 注册：
  - `DYNAMIC_PHOTOGRAPH_FRAMES`
  - `DYNAMIC_PHOTOGRAPH_SETTINGS`
  - `DYNAMIC_PHOTOGRAPH_SUMMARY`
  - `DYNAMIC_PHOTOGRAPH_SESSION_ID`
  - `DYNAMIC_CAMERA_MODE_STATE`

**预期结果**

- 新物品和相机状态具备持久化载体

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 1.4：补齐动态模式相关配置

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\Config.java`

**具体修改**

- 新增服务端配置：
  - 默认采样间隔
  - 最长录制时长
  - 默认动态胶卷最大帧数
- 新增客户端配置：
  - 查看界面默认速度候选项
  - 是否显示详细录制状态

**预期结果**

- 录制间隔、时长上限和容量不再硬编码

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 1.5：先写工具类测试

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\util\DynamicPhotographTimingTest.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\util\DynamicPhotographValidationTest.java`

**新增实现文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\util\DynamicPhotographTiming.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\util\DynamicPhotographValidation.java`

**具体修改**

- 先写失败测试，再补实现
- 覆盖：
  - tick 到秒数换算
  - 最大帧数推导
  - 0 帧失败、1 帧成功规则

**预期结果**

- 最基础的时间与合法性规则有直接测试保护

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:test --tests "*DynamicPhotographTimingTest" --tests "*DynamicPhotographValidationTest"
```

---

## Phase 2：物品与成品封装

目标：先把“动态胶卷”和“动态照片成品”建出来。

### 任务 2.1：实现 `DynamicFilmItem`

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\DynamicFilmItem.java`

**具体修改**

- 定义一卷一段的容量模型
- 支持 tooltip 显示最大帧数和按当前默认采样间隔估算时长
- 支持消耗后失效或转为空状态

**预期结果**

- 动态模式具备专用录制介质

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 2.2：实现 `DynamicPhotographItem`

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\DynamicPhotographItem.java`

**具体修改**

- 读取 `frames/settings/summary/sessionId`
- `use()` 时进入动态照片查看逻辑
- tooltip 显示：
  - 动态照片标识
  - 帧数
  - 采样间隔
  - 估算时长

**预期结果**

- 成品物品具备独立语义，不再复用录像带语义

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 2.3：实现成品封装工厂

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\util\DynamicPhotographFactory.java`

**具体修改**

- 根据 `List<Frame>`、会话参数和封面规则生成 `DynamicPhotographItem`
- 负责同时写入 settings、summary、sessionId

**预期结果**

- 会话结束时可一次性生成正确成品

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 2.4：注册新物品与基础资源

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\Register.java`

**新增资源**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\models\item\dynamic_film.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\models\item\dynamic_photograph.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\lang\en_us.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\lang\zh_cn.json`

**具体修改**

- 注册新物品
- 增加名称与 tooltip 文案
- 添加最小模型资源

**预期结果**

- 新物品可被正常注册并出现在语言资源中

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:processResources
.\gradlew -g .gradle-user-home :common:compileJava
```

---

## Phase 3：服务端会话状态机

目标：先稳定最关键的录制会话生命周期。

### 任务 3.1：定义会话状态与结束原因

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\session\DynamicCaptureSessionEndReason.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\session\DynamicCaptureSessionResult.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\session\DynamicCaptureSession.java`

**具体修改**

- 统一状态流：
  - `STARTING`
  - `RECORDING`
  - `STOPPING`
  - `FINISHED`
- 保存 `sessionId`、玩家、开始 tick、采样间隔、最大帧数、当前 `Frame` 列表

**预期结果**

- 会话生命周期变为可推理的显式模型

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 3.2：实现会话总管

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\session\DynamicCaptureSessionManager.java`

**具体修改**

- 管理玩家到活动会话的映射
- 暴露：
  - `startSession`
  - `getActiveSession`
  - `appendFrame`
  - `requestStop`
  - `finishSession`

**预期结果**

- 玩家同一时间只允许一个动态录制会话

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 3.3：实现逐 tick 推进器

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\session\DynamicCaptureTicker.java`

**具体修改**

- 服务端每 tick 扫描活动会话
- 满足采样时机时发起逐帧请求
- 超时、耗尽、打断时进入停止流程

**预期结果**

- 会话推进从“客户端自说自话”转为“服务端主导合法性”

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 3.4：先写会话状态机测试

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\world\session\DynamicCaptureSessionManagerTest.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\world\session\DynamicCaptureTickerTest.java`

**具体修改**

- 先写失败测试，再补实现
- 覆盖：
  - 松手结束
  - 超时结束
  - 0 帧失败
  - 1 帧成功
  - 胶卷耗尽自动收尾

**预期结果**

- 最危险的会话边界有测试保护

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:test --tests "*DynamicCaptureSessionManagerTest" --tests "*DynamicCaptureTickerTest"
```

---

## Phase 4：动态录制协议与逐帧上传

目标：建立独立于旧普通拍照协议的动态录制协议。

### 任务 4.1：新增动态录制网络包

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\s2c\DynamicCaptureStartS2CP.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\s2c\DynamicCaptureFrameRequestS2CP.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\s2c\DynamicCaptureStateS2CP.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\c2s\DynamicCaptureFrameDataC2SP.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\c2s\DynamicCaptureStopC2SP.java`

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\CommonPackets.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\C2SPackets.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\network\packet\S2CPackets.java`

**具体修改**

- 注册新的动态录制包
- 不继续把多帧录制伪装成普通单拍包

**预期结果**

- 动态录制具备独立协议层

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 4.2：改造客户端采样器

**处理文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\VideoFrameCapture.java`

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\capture\DynamicFrameCaptureClient.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\capture\DynamicFrameUploadQueue.java`

**具体修改**

- 将旧 `VideoFrameCapture` 的图像采样能力下沉到 `DynamicFrameCaptureClient`
- 新增上传队列，负责按帧上传 `ExposureData`
- 旧类仅保留必要兼容层，避免立即大删

**预期结果**

- 画面采样与“长视频缓存”职责分离

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 4.3：接通服务端收帧与 Exposure 落库

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\compatibility\exposure\ExposureAccess.java`

**具体修改**

- 封装对 Exposure `Frame`、`ExposureIdentifier`、`ExposureData`、仓库上传/加载逻辑的访问
- 服务端在收包时：
  - 校验 session
  - 落库 exposure
  - 将 `Frame` 追加到会话

**预期结果**

- 动态录制每帧真正进入 Exposure 的存储链路

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

## Phase 5：相机模式接入与录制控制

目标：让玩家能从现有相机入口触发动态模式。

### 任务 5.1：实现 Dynamic 模式状态对象

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\camera\DynamicCameraModeState.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\DynamicMode.java`

**具体修改**

- 保存当前采样间隔和默认播放速度
- 提供是否装入动态胶卷、是否允许开始录制的判断

**预期结果**

- Dynamic 模式参数不再散落在 UI 和物品逻辑中

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 5.2：实现相机模式控制器与录制触发器

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\camera\DynamicCameraModeController.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\camera\DynamicRecordingTrigger.java`

**具体修改**

- 处理：
  - 动态模式切换
  - 按下开始
  - 持续按住
  - 松手停止
- 与会话总管对接

**预期结果**

- 动态录制交互从会话角度成立

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 5.3：接入当前相机物品

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\item\CamcorderItem.java`

**具体修改**

- 保留当前文件路径，逐步把语义从“录像机”收缩到“动态照片相机入口”
- 接 Dynamic 模式切换、动态胶卷检查、按住快门会话入口
- 保留旧视频逻辑最小编译兼容，避免一次性大删

**预期结果**

- 玩家可以从现有主物品进入 Dynamic Photograph 主链路

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 5.4：补齐 Exposure 接入钩子

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\compatibility\exposure\ExposureCameraHooks.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\exposure_camcorder-common.mixins.json`

**按需新增 mixin/accessor**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\compatibility\mixin\common\*.java`

**修改文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\architectury.common.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\fabric\src\main\resources\fabric.mod.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\neoforge\src\main\resources\META-INF\neoforge.mods.toml`

**具体修改**

- 接入必要的 mixin 配置或 access widener
- 只开放动态模式需要的最小访问面

**预期结果**

- 模组可以安全挂接 Exposure 的相机流程，而不必分叉上游源码

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
.\gradlew -g .gradle-user-home :fabric:compileJava
.\gradlew -g .gradle-user-home :neoforge:compileJava
```

---

## Phase 6：查看界面与播放控制

目标：让动态照片产物可看、可暂停、可调速。

### 任务 6.1：实现播放会话与播放控制器

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\playback\DynamicPlaybackSession.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\playback\DynamicPlaybackController.java`

**具体修改**

- 处理自动播放、暂停、循环、当前帧推进
- 支持“当前查看会话级”速度调整，不回写物品

**预期结果**

- 动态照片播放语义独立成立

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 6.2：实现封面与帧解析器

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\render\DynamicPhotographCoverResolver.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\render\DynamicPhotographFrameResolver.java`

**具体修改**

- 封面优先取第 1 帧
- 封面缺失时回退到下一可用帧
- 全部缺失时使用损坏占位策略

**预期结果**

- 非查看界面和查看界面都具备一致的缺帧容错

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 6.3：扩展当前查看界面

**处理文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\gui\screen\PlaybackScreen.java`

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\gui\screen\DynamicPhotographScreenController.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\gui\screen\DynamicPhotographViewModel.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\gui\component\DynamicPlaybackControls.java`

**具体修改**

- 保留现有查看界面壳
- 把动态播放逻辑下沉到 controller/view model
- 默认打开自动播放，支持手动暂停和会话级调速

**预期结果**

- 查看界面支持动态照片，而不是旧录像播放器语义

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 6.4：接入 Exposure 照片查看钩子

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\compatibility\exposure\ExposurePhotographScreenHooks.java`

**具体修改**

- 让 `DynamicPhotographItem` 可以复用照片查看入口
- 仅在查看界面播放动画，其他地方保持静态封面

**预期结果**

- 动态照片是照片生态里的新物品，而不是完全独立的外部播放器

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

## Phase 7：录制状态 UI 与最小资源补齐

目标：给玩家足够明确的录制反馈。

### 任务 7.1：实现录制状态覆盖层

**新增文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\gui\component\DynamicRecordingStatusOverlay.java`

**具体修改**

- 显示：
  - `REC`
  - 已录帧数
  - 剩余可录时长
  - 剩余可录容量

**预期结果**

- 玩家在录制中能感知状态和边界

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 7.2：补齐界面资源与音效键名

**修改或新增资源**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\textures\gui\viewfinder\recording_indicator.png`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\sounds.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\lang\en_us.json`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\resources\assets\exposure_camcorder\lang\zh_cn.json`

**具体修改**

- 明确区分开始录制、停止录制、动态照片播放相关文案
- 不新增“每帧拍照”那种高频音效语义

**预期结果**

- 资源层表达与新产品语义一致

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:processResources
```

---

## Phase 8：旧录像路径的隔离、回归验证与构建

目标：确保新路径跑通后，不让旧录像术语继续污染主流程。

### 任务 8.1：隔离旧录像持久化路径

**处理文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\storage\Recording.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\storage\RecordingSavedData.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\client\RecordingCache.java`

**具体修改**

- 从主注册与主调用路径中移除对旧录像缓存和录像带存档的依赖
- 暂时保留文件存在，但不让它们成为新主链路的一部分

**预期结果**

- 新动态照片方案不再被旧“录像带”数据模型牵制

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 8.2：冻结投影与世界内回放路径

**处理文件**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\block\ProjectorBlock.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\main\java\io\github\exposure_camcorder\world\block\entity\ProjectorBlockEntity.java`

**具体修改**

- 确保第一版不会把动态照片错误接入世界内动画播放
- 如果必要，仅保留静态兼容或注册隔离

**预期结果**

- 第一版边界保持稳定

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:compileJava
```

---

### 任务 8.3：补全最小测试回归

**新增测试**

- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\world\item\DynamicPhotographFactoryTest.java`
- `D:\apps\appss\fl\HMCL\mod\ExposureCamcorder\common\src\test\java\io\github\exposure_camcorder\client\playback\DynamicPlaybackControllerTest.java`

**具体修改**

- 覆盖：
  - 成品封装 summary/settings/sessionId 正确性
  - 播放自动推进、暂停、循环和速度切换

**预期结果**

- 成品生成和播放控制具备直接测试覆盖

**验证命令**

```powershell
.\gradlew -g .gradle-user-home :common:test
```

---

### 任务 8.4：整体构建与人工运行验证

**验证命令**

```powershell
.\gradlew -g .gradle-user-home build
.\gradlew -g .gradle-user-home :fabric:runClient
```

**人工验收场景**

1. 装入动态胶卷并切到 Dynamic 模式
2. 按住快门录制约 1-2 秒
3. 松手后获得 `DynamicPhotographItem`
4. 打开成品后自动播放
5. 手动暂停并调整本次查看速度
6. 非查看界面只显示封面静帧

**预期结果**

- 第一版最小闭环可用

---

## 执行摘要

| Phase | 目标 | 任务数 | 主要验证 |
|---|---|---:|---|
| 0 | 基线与测试脚手架 | 3 | `:common:test`, `:common:compileJava` |
| 1 | 数据模型与配置 | 5 | `:common:test`, `:common:compileJava` |
| 2 | 物品与成品封装 | 4 | `:common:compileJava`, `:common:processResources` |
| 3 | 服务端会话状态机 | 4 | `:common:test` |
| 4 | 网络与逐帧上传 | 3 | `:common:compileJava` |
| 5 | 相机模式接入 | 4 | `:common:compileJava`, `:fabric:compileJava`, `:neoforge:compileJava` |
| 6 | 查看界面与播放 | 4 | `:common:compileJava` |
| 7 | 录制状态 UI | 2 | `:common:compileJava`, `:common:processResources` |
| 8 | 隔离旧路径与整体验证 | 4 | `:common:test`, `build`, `:fabric:runClient` |

## 关键风险与应对

### 风险 1：Exposure 挂接点不够公开

**应对**

- 优先通过 `ExposureAccess` 封装访问
- 只为 Dynamic 模式增加最小 mixin / accessor
- 不直接修改上游 `Exposure` 源码

### 风险 2：旧录像路径和新动态照片路径互相干扰

**应对**

- 先新增新类，不急着大删旧类
- 到 Phase 8 再做主路径隔离
- 构建通过后再决定是否继续移除废弃代码

### 风险 3：录制状态机边界复杂

**应对**

- 先写 `DynamicCaptureSessionManagerTest` 和 `DynamicCaptureTickerTest`
- 把松手、超时、耗尽、打断全部收敛为统一结束流程

## Gate

计划文档完成后，下一步只能在获得批准后进入 Stage 3 Plan Execution。
