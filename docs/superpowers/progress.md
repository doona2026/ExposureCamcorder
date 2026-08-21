# Development Progress

## Stage 1 Requirement Exploration
- Started at: 2026-08-20
- Design doc: `docs/superpowers/specs/2026-08-20-version-1.20.1-dual-line-design.md`
- Scope: keep the current 1.21.1 line and add a `version/1.20.1` independent Fabric + Forge line

## Stage 2 Implementation Planning
- Started at: 2026-08-20
- Plan doc: `docs/superpowers/plans/2026-08-20-version-1.20.1-dual-line-plan.md`
- Status: draft, awaiting approval

## Stage 1 Requirement Exploration
- Completed at: 2026-08-19
- Design doc: `docs/superpowers/specs/2026-08-19-forge-47x-fabric-dual-line-design.md`
- Scope: keep the current 1.21.1 line and add a 1.20.1 + Forge 47.x + Fabric 1.20.1 line

## Stage 2 Implementation Planning
- Completed at: 2026-08-19
- Plan doc: `docs/superpowers/plans/2026-08-19-forge-47x-fabric-dual-line-plan.md`
- Status: approved

## Stage 3 Plan Execution
- Started at: 2026-08-19
- Current progress: all 13 tasks complete; see the 1.20.1 plan-execution note below.
- Completed: 1.20.1 dual-line build (Fabric + Forge 47) and 1.21.1 root regression all pass.

## Stage 1 Requirement Exploration
- Completed at: 2026-05-14
- Design doc: `docs/dynamic-photograph-architecture_zh.md`

## Stage 2 Implementation Planning
- Completed at: 2026-05-14
- Plan doc: `docs/2026-05-14-dynamic-photograph-plan.md`

## Stage 3 Plan Execution
- Started at: 2026-05-14
- Current progress:
  - Bootstrapped a Gradle + Architectury-style workspace with wrapper and module build files.
  - Implemented `common` entrypoints, config, registration facade, dynamic photograph data components, utility classes, item classes, and initial unit tests.
  - Completed Phase 3 session lifecycle work: `DynamicCaptureSession`, `DynamicCaptureSessionManager`, `DynamicCaptureTicker`, and session tests.
  - Started Phase 4 protocol work: dedicated dynamic capture packet definitions, client frame capture/upload queue, and `ExposureAccess`.
  - Continued into Phase 5 common-side control flow: `DynamicMode`, `DynamicCameraModeController`, `DynamicRecordingTrigger`, `CamcorderItem`, and `ExposureCameraHooks`.
  - Added Phase 5.4 client-side access surface for future screen integration via `PhotographScreenAccessor`.
  - Added Phase 6.1 playback core: `DynamicPlaybackSession`, `DynamicPlaybackController`, and playback tests.
  - Added Phase 6.2 frame resolving core: `DynamicPhotographCoverResolver`, `DynamicPhotographFrameResolver`, and fallback tests.
  - Added Phase 6.3 view-state layer: `DynamicPhotographScreenController`, `DynamicPhotographViewModel`, `DynamicPlaybackControls`, and screen/controller tests.
  - Added Phase 6.4 Exposure viewing hook: `ExposurePhotographScreenHooks`, `PhotographScreenMixin`, and dynamic photograph item screen wiring.
  - Added Phase 6.5 playback control widgets via `ScreenAccessor` and `DynamicPlaybackControls` screen integration.
  - Added Phase 6.6 static cover tooltip image support for `DynamicPhotographItem`.
  - Added Phase 7.1 recording status overlay via `DynamicRecordingStatusOverlay`.
  - Added Phase 7.2 recording/viewing localization keys and sound events, plus the recording indicator texture.
  - Added Phase 8.3 regression tests for `DynamicPhotographFactory` and playback speed changes.
  - Added a pure data creation helper in `DynamicPhotographFactory` so the factory packaging path is testable without platform registration.
  - Verified `:common:test`, `:common:compileJava`, and `:common:processResources`.
  - Left `fabric` and `neoforge` source skeletons on disk, but temporarily excluded them from `settings.gradle` until the platform build wiring is completed.

## Stage 3 Plan Execution (1.20.1 dual-line)
- Completed: all tasks done; Fabric, Forge 47, and 1.21.1 regression builds pass.
