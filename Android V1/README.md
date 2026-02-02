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

## V1 约定

- V1 only operates on explicitly selected text（不做前后文）。
- 解释失败时浮窗内显示人话提示 + 关闭，不做重试。
- API Key 仅存于本机，不写死、不提交仓库。
