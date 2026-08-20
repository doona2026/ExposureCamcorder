# Development Progress

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
