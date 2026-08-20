# 1.20.1 Forge 47.x + Fabric 双版本线实施计划

## 目标与固定版本

- 保留根目录现有 Minecraft 1.21.1 `common` / `fabric` / `neoforge` 构建和源码结构。
- 新增独立构建 `versions/1.20.1`，包含 `common` / `fabric` / `forge` 三个模块。
- Minecraft: `1.20.1`
- Java: `17`
- Forge: `1.20.1-47.4.10`
- Fabric Loader: `0.16.10`
- Fabric API: 从本地源码 `D:/apps/appss/fl/HMCL/mod/fabric-api-1.20.1` 发布到 `versions/1.20.1/local-maven`，版本 `0.92.11+local-main`
- Exposure: `io.github.mortuusars.exposure:exposure-1.20.1-{common,fabric,forge}:1.9.20`
- Forge Config API Port: `8.0.1`
- Parchment: `1.20.1:2023.09.03`

## 架构边界

根目录继续代表 1.21.1 发布线。`versions/1.20.1` 是独立 Gradle 构建，使用自己的版本属性、Java 级别、平台模块和资源元数据。两条线不共享 Minecraft 编译类路径；仅在移植时复制稳定的业务逻辑和资源，防止 1.20.1 的 Forge/Loom 配置影响现有 NeoForge 1.21.1。

1.20.1 的物品数据统一通过 `DynamicItemStackData` 读写 NBT。网络统一使用 1.20.1 的 `FriendlyByteBuf` 和平台接收器，不引用 1.21.1 的 `CustomPacketPayload`、`StreamCodec` 或数据组件 API。

## 执行任务

1. 建立两条版本线的基线。
   - 检查文件：`settings.gradle`、`build.gradle`、`gradle.properties`、`common/build.gradle`、`fabric/build.gradle`、`neoforge/build.gradle`。
   - 运行：`./gradlew.bat :common:test :common:compileJava :fabric:compileJava :neoforge:compileJava`。
   - 预期：记录当前 1.21.1 的成功项和已有失败项；后续不得新增根构建回归。

2. 创建 1.20.1 独立 Gradle 骨架。
   - 新增：`versions/1.20.1/settings.gradle`、`versions/1.20.1/build.gradle`、`versions/1.20.1/gradle.properties`。
   - 新增：`versions/1.20.1/gradlew`、`versions/1.20.1/gradlew.bat`、`versions/1.20.1/gradle/wrapper/*`，固定 Gradle `8.14`。
   - 新增：`versions/1.20.1/common/build.gradle`、`versions/1.20.1/fabric/build.gradle`、`versions/1.20.1/forge/build.gradle`。
   - 使用本地 Fabric API 源码的 `publish` 任务把聚合包和全部子模块发布到 `versions/1.20.1/local-maven`，主构建优先从该仓库解析 `net.fabricmc.fabric-api:fabric-api:0.92.11+local-main`。
   - 运行：`versions/1.20.1/gradlew.bat -p versions/1.20.1 projects`。
   - 预期：显示 `common`、`fabric`、`forge`，并能解析本地 Fabric API 构建。

3. 建立 1.20.1 公共源码和资源基线。
   - 新增目录：`versions/1.20.1/common/src/main/java/io/github/exposure_camcorder`。
   - 新增目录：`versions/1.20.1/common/src/test/java/io/github/exposure_camcorder`。
   - 新增目录：`versions/1.20.1/common/src/main/resources`。
   - 复制当前纯业务逻辑、测试、材质、模型、语言、音效和标签作为移植起点。
   - 将 1.20.1 数据目录改为 `recipes`、`advancements`、`tags/items`，并把配方结果字段由 `id` 改为 `item`、移除 1.20.1 不支持的 advancement 字段。
   - 运行：`./gradlew.bat -p versions/1.20.1 :common:processResources`。
   - 预期：资源处理完成，输出中不存在未展开变量和重复资源错误。

4. 先测试后实现 1.20.1 NBT 数据层。
   - 新增测试：`versions/1.20.1/common/src/test/java/io/github/exposure_camcorder/world/data/DynamicItemStackDataTest.java`。
   - 测试覆盖：默认值、完整往返、缺失字段、损坏字段、相机模式、胶卷已用帧数和最大帧数。
   - 新增实现：`versions/1.20.1/common/src/main/java/io/github/exposure_camcorder/world/data/DynamicItemStackData.java`。
   - 修改记录类：`world/component/DynamicPhotographFrames.java`、`DynamicPhotographSettings.java`、`DynamicPhotographSummary.java`、`DynamicSessionId.java`、`world/item/camera/DynamicCameraModeState.java`，保留 `Codec`，移除 `StreamCodec`。
   - 运行红灯：`./gradlew.bat -p versions/1.20.1 :common:test --tests '*DynamicItemStackDataTest'`，预期因实现缺失失败。
   - 运行绿灯：同一命令，预期全部通过。

5. 回移公共注册和物品逻辑。
   - 修改：`versions/1.20.1/common/src/main/java/io/github/exposure_camcorder/ExposureCamcorder.java`、`Register.java`、`PlatformHelper.java`、`Config.java`。
   - 修改：`world/item/DynamicFilmItem.java`、`DevelopedDynamicFilmItem.java`、`DynamicPhotographItem.java`、`world/item/camera/DynamicCameraModeController.java`、`DynamicRecordingTrigger.java`。
   - 删除 1.20.1 不存在的数据组件注册；Creative Tab 使用 1.20.1 构造 API；所有 ItemStack 数据访问改为 `DynamicItemStackData`。
   - 运行：`./gradlew.bat -p versions/1.20.1 :common:compileJava`。
   - 预期：公共注册、物品和相机会在 Java 17 / Minecraft 1.20.1 下编译。

6. 回移动态照片创建、播放和导出逻辑。
   - 修改：`util/DynamicPhotographFactory.java`、`client/render/DynamicPhotographCoverResolver.java`、`client/gui/screen/DynamicPhotographScreenController.java`、`client/playback/DynamicPhotographDisplayPlaybackManager.java`、`client/export/DynamicPhotographGifExport.java`、`compatibility/exposure/ExposurePhotographScreenHooks.java`。
   - 把当前 `ItemStack.getOrDefault/set` 数据组件调用替换为 NBT 数据层；把 `List.getFirst()` 改为 Java 17 兼容访问。
   - 使用 Exposure 1.9.20 的 `Exposure.DataComponents` NBT 方法写入静态照片帧和照片类型。
   - 运行：`./gradlew.bat -p versions/1.20.1 :common:test :common:compileJava`。
   - 预期：现有播放、时间、会话、帧选择和工厂测试在 1.20.1 线通过。

7. 先测试后回移网络协议定义。
   - 新增测试：`versions/1.20.1/common/src/test/java/io/github/exposure_camcorder/network/DynamicPacketCodecTest.java`。
   - 修改：`network/packet/HandledPayload.java`、`C2SPackets.java`、`S2CPackets.java` 及 `network/packet/c2s`、`network/packet/s2c` 下全部包类型。
   - 每个包提供 `ResourceLocation ID`、`write(FriendlyByteBuf)` 和静态 `read(FriendlyByteBuf)`；移除 `CustomPacketPayload`、`TypeAndCodec`、`StreamCodec`。
   - 运行红灯后再运行绿灯：`./gradlew.bat -p versions/1.20.1 :common:test --tests '*DynamicPacketCodecTest'`。
   - 预期：所有 C2S/S2C 字段可无损编码解码，并拒绝超长帧数据。

8. 回移 Exposure 1.9.20 兼容层和 Mixin。
   - 修改：`versions/1.20.1/common/src/main/java/io/github/exposure_camcorder/compatibility/exposure/ExposureAccess.java`、`ExposureCameraHooks.java`、`ExposurePhotographScreenHooks.java`、`ExposureViewfinderOverlayHooks.java`、`PhotographScreenAccess.java`。
   - 新增并调整 Fabric Mixin：`versions/1.20.1/fabric/src/main/java/io/github/exposure_camcorder/compatibility/mixin/fabric/*.java`。
   - 新增并调整 Forge Mixin：`versions/1.20.1/forge/src/main/java/io/github/exposure_camcorder/compatibility/mixin/forge/*.java`。
   - 以 Exposure `origin/1.20.1-1.9-backport` 的类名、方法签名和字段为准，逐个校准注入点。
   - 运行：`./gradlew.bat -p versions/1.20.1 :fabric:compileJava :forge:compileJava`。
   - 预期：所有 Mixin 目标类和方法描述符可解析，平台源码完成编译。

9. 实现 Fabric 1.20.1 平台入口。
   - 新增：`versions/1.20.1/fabric/src/main/java/io/github/exposure_camcorder/fabric/ExposureCamcorderFabric.java`、`ExposureCamcorderFabricClient.java`、`RegisterImpl.java`、`PlatformHelperImpl.java`。
   - 新增：`versions/1.20.1/fabric/src/main/java/io/github/exposure_camcorder/fabric/network/FabricPackets.java`、`fabric/client/DynamicCameraClientInput.java`。
   - 使用 Fabric Networking API v1 的全局接收器、客户端接收器、服务器生命周期和客户端 tick/key binding 事件。
   - 运行：`./gradlew.bat -p versions/1.20.1 :fabric:compileJava :fabric:remapJar`。
   - 预期：生成 `versions/1.20.1/fabric/build/libs/exposure_camcorder-fabric-1.20.1-0.1.0.jar`。

10. 实现 Forge 47.x 平台入口。
    - 新增：`versions/1.20.1/forge/src/main/java/io/github/exposure_camcorder/forge/ExposureCamcorderForge.java`、`RegisterImpl.java`、`PlatformHelperImpl.java`。
    - 新增：`versions/1.20.1/forge/src/main/java/io/github/exposure_camcorder/forge/network/ForgePackets.java`、`forge/client/DynamicCameraClientInputForge.java`。
    - 使用 Forge 47 `DeferredRegister`、`SimpleChannel`、mod event bus、Forge event bus 和客户端按键事件。
    - 运行：`./gradlew.bat -p versions/1.20.1 :forge:compileJava :forge:remapJar`。
    - 预期：生成 `versions/1.20.1/forge/build/libs/exposure_camcorder-forge-1.20.1-0.1.0.jar`。

11. 完成 1.20.1 元数据、Mixin 配置和平台资源。
    - 新增：`versions/1.20.1/common/src/main/resources/exposure_camcorder.accesswidener`、`exposure_camcorder-common.mixins.json`。
    - 新增：`versions/1.20.1/fabric/src/main/resources/fabric.mod.json`、`exposure_camcorder-fabric.mixins.json`。
    - 新增：`versions/1.20.1/forge/src/main/resources/META-INF/mods.toml`、`exposure_camcorder-forge.mixins.json`、`pack.mcmeta`。
    - 元数据固定声明 Minecraft 1.20.1、Java 17、Fabric API/Fabric Loader 或 Forge 47、Exposure 1.9.20 和 Forge Config API Port 8.0.1。
    - 运行：`./gradlew.bat -p versions/1.20.1 :fabric:processResources :forge:processResources`。
    - 预期：展开后的 JSON/TOML 可解析，Mixin compatibility level 为 `JAVA_17`。

12. 执行完整 1.20.1 验证。
    - 运行：`./gradlew.bat -p versions/1.20.1 clean test build`。
    - 检查两个发布 JAR 的 `fabric.mod.json` / `META-INF/mods.toml`、Mixin JSON、物品模型、语言、配方和 advancement 路径。
    - 启动 Fabric 开发客户端并检查主菜单日志：`./gradlew.bat -p versions/1.20.1 :fabric:runClient`。
    - 启动 Forge 开发客户端并检查主菜单日志：`./gradlew.bat -p versions/1.20.1 :forge:runClient`。
    - 预期：两个加载器均进入主菜单，不出现缺失类、Mixin 注入失败、注册冻结或网络通道错误。

13. 回归验证 1.21.1 并补充构建说明。
    - 新增：`versions/1.20.1/README.md`，记录本地 Fabric API 路径、Java 17、构建命令和产物位置。
    - 更新：`docs/superpowers/progress.md`。
    - 运行：`./gradlew.bat :common:test :common:compileJava :fabric:compileJava :neoforge:compileJava`。
    - 预期：根目录 1.21.1 的结果不差于任务 1 基线；1.20.1 两个平台 JAR 均保留。

## 验收标准

- 根目录仍可作为 1.21.1 Fabric + NeoForge 工程使用。
- `versions/1.20.1` 可独立构建 Forge 47.x 和 Fabric 1.20.1。
- Fabric 构建使用由 `D:/apps/appss/fl/HMCL/mod/fabric-api-1.20.1` 源码生成的本地 Maven 产物。
- 1.20.1 动态照片、相机模式和胶卷状态使用 NBT 持久化，保存重载后数据不丢失。
- C2S/S2C 动态录制协议在两个加载器上完成注册和收发。
- 两个平台开发客户端均无 Mixin、注册和依赖加载错误。
- 新增测试和两条版本线的相关构建全部通过；Minecraft 客户端验证替代浏览器验证，本任务不包含 Web UI。
