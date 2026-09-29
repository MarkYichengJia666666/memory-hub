# Privy 上报 image1/image2：七牛 key 原样透出，勿当 Base64 再传

## decision
完件/补件 Privy 上传表单（`UploadLivingInfoPrivyRequest` / `PrivyLivingUploadForm`）的 `image1`/`image2` 语义为**端上已传七牛后的存储 key**，不是 Base64。`PrivyLivingSdkService` 影像通道应**原样透出 key**（落库保留 `image1Key`/`image2Key`），去掉「Base64 解码 + `LivingDataService.savePicture` 二次上传」。完件 Cover 与补件 `livenessDetectionByPrivy` 共用同一表单口径。
若历史上仍见 `data:image/jpeg;base64,` payload：那是旧入参形态，解码须先剥 data URI 前缀（且优先只在 Privy 入口剥，避免改全局 `savePicture`）；与「现网已改 key」后的主路径分开看。

## workspace
- `TAPD-373401-privy-liveness-app-api`
- `TAPD-1373384-living-provider-abstraction`

## mouths
- Privy 上传 / 七牛 key
- 完件与补件活体

## anchors
- symbols: `UploadLivingInfoPrivyRequest`, `PrivyLivingUploadForm`, `PrivyLivingSdkService`, `LivenessDetectionController#livenessDetectionByPrivy`
- routes: `/api/loan/v8/uploadLivingInfo`, `/api/secure/livenessDetection`

## constraints
- 勿再对 key 做 Base64 解码
- fcToken/image 不落库；请求日志须脱敏（见姊妹 seed）

## evidence
- Claude×ec-01 · session `718f1d79`
- Claude×ec-01 · session `5dad8f7a`

## status
active

## updated
2026-09-28
