InlineNote · Technical Foundation Document (v0.1)
1. 背景与动机（Context & Motivation）

在多个知识领域的学习过程中（哲学、建筑、人工智能），创作者反复遭遇同一种障碍：
高度专业化、内部化的语言系统被用来制造理解门槛，而非促进沟通。

哲学中的概念循环与抽象回指

建筑文本中自我指涉的设计黑话

AI / 编程领域中大量默认前置知识的缩写与术语

这些语言并非不可理解，但现有理解路径（复制、搜索、切换应用）会显著打断思考与阅读流，导致理解成本被无限放大。

InlineNote 旨在进行一项极小但明确的干预：

在用户阅读的当下，把一句难以理解的话拉回到可理解的层面。

2. 问题定义（Problem Definition）

当用户在 Android 设备上阅读文本时：

遇到一段难以理解的句子（黑话、套话、隐性前提）

用户希望立即理解其含义

但不希望：

复制粘贴

跳转应用

打断当前阅读上下文

当前解决方式在体验上存在系统性问题：

方式	问题
复制 → 搜索	上下文丢失、噪音高
复制 → AI 聊天	操作成本高、割裂阅读
忍过去	理解中断、认知负债
问他人	非即时、有社交成本
3. 产品原则（Non-negotiable Principles）

InlineNote 在设计与技术实现上遵循以下不可妥协原则：

3.1 体验原则

In-situ：解释必须发生在原始阅读界面中

Low friction：不允许复制、粘贴或切 App

User-triggered：不自动解释，不主动打断

Minimalism：一次只解释一小段内容

3.2 技术态度

不追求“自动理解一切”

不假设用户是新手或专家

不试图替代原文本，而是临时补充理解

4. 产品形态（Product Form）

InlineNote 是一个 Android 系统级辅助工具，而非传统应用。

基本交互流程（V1）
用户在任意 App 中阅读
→ 选中一段文本
→ 出现轻量“Explain / 解释”触发按钮
→ 用户点击
→ 原地浮窗显示解释
→ 关闭后继续阅读

关键点

“选中”被视为可接受的最小前序动作

解释结果不替换原文、不侵入原界面

浮窗为一次性、可关闭的临时存在

5. 技术架构概览（High-level Architecture）
5.1 系统能力选择

Android Accessibility Service 被选为核心技术路径，原因：

唯一合法、稳定获取跨 App 文本选择事件的方式

可监听文本选中变化

可在不修改宿主 App 的情况下介入

5.2 核心模块
[Accessibility Service]
        │
        ├─ Text Selection Listener
        │
        ├─ Trigger UI (Floating Button)
        │
        ├─ LLM Request Handler
        │
        └─ Overlay Explanation Window

6. 核心模块说明
6.1 Accessibility Service

监听 TYPE_VIEW_TEXT_SELECTION_CHANGED

获取当前选中文本内容

做最基本过滤（长度、语言、空白等）

6.2 触发机制（Trigger）

不自动弹解释

仅显示一个轻量浮动按钮（如 “解释”）

用户点击后才进入解释流程

6.3 解释生成（LLM Interaction）

V1 仅使用单轮、强约束请求：

输入：

选中文本

（可选）前后极少量上下文

输出：

大白话解释

要点拆解

（可选）潜台词提示

不涉及：

多轮对话

agent 调度

工具调用

6.4 展示层（Overlay）

系统级浮窗

不遮挡原文

可拖拽、可关闭

不持久保存

7. 明确不做的事情（Explicit Non-Goals）

在 V1 阶段，InlineNote 明确不包含：

❌ 自动判断用户是否“看不懂”

❌ 批量文本分析

❌ 长文档摘要

❌ 用户画像 / 个性化

❌ agent 自动执行行为

❌ 内容替换或标注写回原 App

这些能力被视为潜在演化方向，而非初始目标。

8. 成功判据（Success Criteria）

不使用宏观指标，仅使用可体验判断：

用户可在 3 秒内完成“选中 → 理解 → 回到阅读”

使用后无需额外操作即可继续原任务

功能第一次使用无需教程即可理解用途

用户不会感到被打断或被“教育”

9. 未来演化方向（非承诺）

InlineNote 在保持核心体验不变的前提下，可能演化为：

多解释模式（大白话 / 潜台词 / 怎么回应）

轻量上下文扩展（前后句）

Human-in-the-loop micro-agent（仅建议，不自动执行）

但任何演化不得破坏现有体验原则。

10. 总结

InlineNote 并非试图解决“理解问题”本身，而是：

在理解被语言门槛阻断的那一刻，
提供一个不打断、不越权、不自作聪明的补丁。

它是一项微小、克制、但长期存在的工作。

---

11. 工程结构（Project Structure）

**语言**：Kotlin（Android 官方推荐，新项目默认选择）。

**根目录（Android 工程）**

```
Android V1/
├── app/                      # 主应用模块
├── gradle/
├── build.gradle.kts          # 工程级 Gradle 配置
├── settings.gradle.kts
├── TechDoc.md                # 本文档
└── README.md                 # 本地构建与运行说明（可选）
```

**app 模块内包与目录（与 §5.2 / §6 对应）**

| 包/目录 | 对应文档 | 职责 |
|--------|----------|------|
| `service/` | §6.1 | Accessibility Service：监听 `TYPE_VIEW_TEXT_SELECTION_CHANGED`，获取选中文本，做长度/语言/空白等基本过滤 |
| `trigger/` | §6.2 | 触发 UI：选中后显示轻量浮动按钮（如「解释」），用户点击后才进入解释流程 |
| `llm/` | §6.3 | 解释生成：定义「解释(文本) → 结果」的接口，实现直连 OpenAI 或经后端转发，便于后续切换 API 来源 |
| `overlay/` | §6.4 | 展示层：系统级浮窗，展示解释内容，可拖拽、可关闭，不持久保存 |

**app 内建议的 Kotlin 包结构示例**

```
app/src/main/
├── kotlin/.../inlinenote/
│   ├── service/               # Accessibility
│   │   └── InlineNoteAccessibilityService.kt
│   ├── trigger/               # 浮动按钮
│   │   └── FloatingTriggerView.kt
│   ├── llm/                   # 解释接口与实现
│   │   ├── Explainer.kt       # 接口
│   │   ├── ExplainResult.kt   # 结果模型
│   │   ├── OpenAIExplainer.kt # 直连 OpenAI（可替换为 BackendExplainer）
│   │   └── ...
│   ├── overlay/               # 解释浮窗
│   │   └── ExplanationOverlay.kt
│   └── InlineNoteApplication.kt
├── res/
└── AndroidManifest.xml
```

**设计约定**：LLM 调用通过 `LLMClient` 抽象，业务层不依赖具体 API（直连或后端）。后续更换或增加解释来源时，仅替换/新增 `llm/` 下实现，不改触发与浮窗逻辑。

---

12. V1 实现决策（Implementation Decisions）

V1 的构建决策已单独成文，实现时以该文档为准。

**文档**：[V1builddecision.md](./V1builddecision.md)

**结论摘要**（与 §11 一致）：

| 项 | V1 决策 |
|----|--------|
| LLM 连接 | App 直连 OpenAI；`llm/` 内接口 `LLMClient`，实现 `OpenAIClientDirect` |
| 前后文 | 不做；V1 only operates on explicitly selected text. |
| API Key | 用户 App 内输入，Keystore / EncryptedSharedPreferences 本地加密，不写死、不提交仓库 |
| 版本与权限 | minSdk 24，targetSdk 34；最小无障碍引导（说明 + 跳转设置） |
| 浮窗/按钮 UI | 先可用：看得清、点得到、关得掉 |
| 包名 | `com.inlinenote.android` |
| 错误处理 | 浮窗内提示（人话文案），提供「关闭」，不做重试 |