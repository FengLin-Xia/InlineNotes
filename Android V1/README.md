# InlineNote · Android V1

在任意 App 中选中文字 → 出现「解释」按钮 → 点击后浮窗显示大白话解释。详见 [TechDoc.md](./TechDoc.md) 与 [V1builddecision.md](./V1builddecision.md)。

## 环境要求

- Android Studio Ladybug (2024.2.1) 或更高
- JDK 17
- minSdk 24，targetSdk 34

## 构建与运行

1. 用 Android Studio 打开本目录（`Android V1`），等待 Gradle 同步完成。
2. 连接真机或启动模拟器，点击 Run。
3. 首次打开：在应用内输入 OpenAI API Key 并保存；按提示开启「无障碍」与「悬浮窗」权限。
4. 在系统设置中为 InlineNote 开启无障碍服务。
5. 在任意可选中文字的应用中选中一段文字，应出现「解释」浮动按钮；点击后等待解释浮窗。

## 工程结构（简要）

- `app/src/main/kotlin/com/inlinenote/android/`
  - `service/` — Accessibility Service（监听选中、过滤、协调触发与浮窗）
  - `trigger/` — 浮动「解释」按钮
  - `llm/` — `LLMClient` 接口与 `OpenAIClientDirect` 实现（直连 OpenAI）
  - `overlay/` — 解释/错误浮窗
- `MainActivity` — API Key 输入、无障碍与悬浮窗引导
- `KeyStoreHelper` — API Key 本地加密存储（EncryptedSharedPreferences）

## 传给别人 / 分发

### 方式一：打一个 APK 直接发（最简单）

1. 在 Android Studio 里：**Build → Build Bundle(s) / APK(s) → Build APK(s)**。
2. 构建完成后点 **Locate**，或到目录  
   `Android V1/app/build/outputs/apk/debug/app-debug.apk`  
   把这个 **app-debug.apk** 发给对方（网盘、微信、USB 等）。
3. 对方在手机上：
   - 若系统提示「禁止安装未知来源应用」，去 **设置 → 安全 / 应用管理** 里允许「来自此来源」或「未知来源」安装；
   - 用文件管理器找到并点击 APK，按提示安装；
   - 打开 InlineNote，按应用内提示配置 **API Key**、开启 **无障碍** 和 **悬浮窗** 权限。

Debug 包用的是 Android Studio 自带的调试签名，安装没问题，只是不适合上架应用商店。

### 方式二：签正式名再发（可选）

若希望包名固定、以后升级不丢数据，可用自己的密钥签 Release 包。

**若提示「无法加载 key store」「系统找不到指定的文件」：说明 keystore 还没创建，先做下面第 1 步。**

1. **先创建 keystore 文件（仅第一次）**  
   在 **PowerShell** 或 **Android Studio Terminal** 里，先 `cd` 到项目目录，再执行（若系统找不到 `keytool`，用下面带完整路径的那行）：
   ```powershell
   cd "C:\Users\Fenglin\InlineNotes\Android V1"
   keytool -genkey -v -keystore inlinenote-release.keystore -alias inlinenote -keyalg RSA -keysize 2048 -validity 10000
   ```
   若提示「无法将 keytool 项识别为…」，改用 Android Studio 自带的 keytool 完整路径：
   ```powershell
   & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkey -v -keystore inlinenote-release.keystore -alias inlinenote -keyalg RSA -keysize 2048 -validity 10000
   ```
   （需已在该 PowerShell 里执行过 `cd "C:\Users\Fenglin\InlineNotes\Android V1"`，这样 keystore 会生成在当前目录。）
   按提示输入密钥库密码、姓名等（可随意填，自己记住即可）。执行成功后，当前目录下会出现 **inlinenote-release.keystore**。请妥善保管该文件和密码，勿提交到 Git。

2. 菜单 **Build → Generate Signed Bundle / APK** → 选 **APK** → **Next** → **Choose existing...**，在弹窗里选中刚生成的 **inlinenote-release.keystore**（路径即 `Android V1\inlinenote-release.keystore`），输入密码和 alias 密码（若上一步没单独设 key 密码，与 keystore 密码相同），**Key alias** 填 `inlinenote` → **Next** → 选 **release** → **Finish**。

3. 签好名的 APK 在 `app/build/outputs/apk/release/`。

对方安装方式同方式一。

### 对方使用前必做

- 在应用内填写并保存 **API Key**（及可选 Base URL、Model）。
- 在系统设置中为 **InlineNote** 开启 **无障碍** 服务。
- 若提示需要 **悬浮窗** 权限，到系统设置里为本应用开启。

---

## V1 约定

- V1 only operates on explicitly selected text（不做前后文）。
- 解释失败时浮窗内显示人话提示 + 关闭，不做重试。
- API Key 仅存于本机，不写死、不提交仓库。
