# Artflux repository review

Reviewed state: `chore/artflux-pre-transfer` (2026-11-05), following the reliability, home/library, discovery/storage, health/storage-viewer, and identity passes (pull requests #1–#4 and the pre-transfer branches).

Pre-transfer preparation for moving Artflux into a fresh GitHub repository. Completed work, remaining work, and items that require manual verification are separated below.

## Completed in previous passes

| Area | Outcome |
| --- | --- |
| Feed reliability | Source edits apply immediately, draft search is kept separate from the submitted query, filters and sorting apply across pages, same-page duplicates are deduplicated, refresh failures keep artwork visible, and retries are bounded and cancellable. |
| Transport of diagnostics | Cancelled coroutines cancel the actual OkHttp call; stale diagnostic results are discarded. |
| Credential security | Credentials are AES-GCM encrypted with an Android Keystore key, never fall back to plaintext, and persistence failures are reported honestly. |
| Home & library (PR #2) | Home/Library/Settings navigation, three themes, per-source submitted search/filters/scroll, tag blocking, library search and bulk actions, durable DownloadManager transfers, offline originals, JSON backup/restore without secrets, and source-aware library keys with migration. |
| Discovery & storage (PR #3) | Source health status with recovery-aware cooldowns, offline storage manager, improved viewer (paging, zoom, similar-artwork), and related-artwork matching with cancellation. |
| Health/storage viewer (PR #4) | Compact viewer errors and storage capture before confirmation dismissals. |

## Completed in this pass (pre-transfer)

| Finding | Change |
| --- | --- |
| The Android identity was still `com.example` / `com.aistudio.mediabrowser.kpxz` from the AI Studio template. | Adopted the permanent identity `com.swartzz.artflux` for namespace and applicationId; all package declarations, imports, source folders, tests, and resource references moved with the rename. Remaining `MediaBrowser*` type names are app code, not template identity. |
| `metadata.json` was an AI Studio template project descriptor. | Removed after verifying nothing (build, CI, manifest, docs) referenced it. |
| `.env.example` carried an AI Studio template comment. | Rebranded to Artflux. |
| CI triggered on obsolete branches (`master`, `codex/artflux-reliability`, `codex/artflux-home-library`). | Workflows now trigger on `main` pushes, `main` pull requests, and manual dispatch, matching the current branch structure. |
| Credentials could be transmitted over public cleartext HTTP. | New transport-security policy: authentication query parameters and headers are attached only for HTTPS endpoints or local/private network hosts (`localhost`, `127.x`, `10.x`, `172.16–31.x`, `192.168.x`, `169.254.x`, `*.local`, `*.localhost`). On public HTTP hosts the credentials are withheld, and diagnostics show a warning step instead of a misleading authentication failure. |
| HTTP→HTTPS media URL conversion was undocumented. | Reviewed and kept as designed: parsing upgrades `http://` media URLs to https (covered by regression tests). The manifest still allows cleartext deliberately so genuine HTTP-only custom sources and local network sources keep working. |

## Remaining recommendations

1. **Move saves and collections to Room.** The JSON SharedPreferences store still rewrites full lists on the main thread; a database would support atomic collection updates, large libraries, and responsive search, with a tested JSON import migration.
2. **Finish download accuracy.** Filenames and formats are handled through DownloadManager, but the download-quality labels (for example Original vs 1080p) should reflect what each provider actually serves; add download history and optional Wi-Fi-only transfers.
3. **Extend library tools.** Backup covers metadata; offline file export, storage-based sorting refinements, and richer bulk actions remain ideas, not committed features.
4. **Accessibility and maintainability.** Extract user-facing strings for localization, audit contrast/touch targets and TalkBack, and split the large view-model/settings/test files by feature as they change.
5. **Cleartext surface.** Cleartext traffic stays globally enabled by design for custom and local sources. If a stricter model is ever wanted, it should be a network-security-config with a single user-visible toggle; do not silently break existing sources.

## Verification (this pass)

- `testDebugUnitTest`: **132 tests, 0 failures**, including the new `TransportSecurityTest` covering the credential-withholding policy, local/private host detection, and request building for HTTPS, public HTTP, and LAN HTTP endpoints.
- `lintDebug`: passes.
- `assembleDebug`: builds; the produced APK declares applicationId `com.swartzz.artflux`.
- No old identity references remain in any tracked file.

Passing CI and local runs do not replace real-device testing.

## Manual verification checklist (physical device)

1. **Existing saved artwork and collections**
   - Open Library → Saved, Collections, Offline, Downloads after updating from the previous install; all saved metadata and collection membership must remain.
   - Run Settings → Backup export once before reinstalling or switching installs.
2. **Media loading**
   - Browse the built-in sources (Safebooru, Danbooru, Yande.re, waifu.im): images, GIFs, and videos load in feeds and the viewer.
   - Blur/filter toggles, deep scroll restore after a process restart, and pull-to-refresh behave.
3. **Downloads**
   - Start a gallery download, background the app, and confirm the queue survives; retry after failure; delete offline files from the storage manager and confirm records/files agree.
   - On Android 7–9, confirm storage permission is requested for gallery downloads.
4. **Source authentication**
   - Save an authenticated source, restart the app, and confirm authentication survives.
   - Run full diagnostics on an authenticated HTTPS source: authentication step must pass with real credentials.
   - Configure a source using a public `http://` endpoint with credentials: diagnostics must show the transport-security warning, and browsing must behave like an unauthenticated request. Switch to HTTPS, or verify on a LAN host (for example `http://192.168.x.x`) that credentials are sent again.

## Migration readiness

The project is ready to migrate into a fresh GitHub repository on this branch. Nothing in the repository depends on the AI Studio template or the old application id; CI only triggers on `main`; the documentation carries no stale repository-specific findings. `local.properties` is gitignored, so no machine-specific paths travel with the repository.
