InlineNote · V1 Build Decisions（建议定稿）

下面我按你的 1–7 点来，每一项给出：

V1 建议选择

为什么

如果以后要换，成本如何

1. LLM 连接方式（A or B）
✅ V1 建议：A：App 直连 OpenAI

理由：

你现在做的是 个人 / 原型 / 产品验证

B 会立刻引入：

服务器部署

鉴权

API 设计

运维心智负担

这些 和 InlineNote 的核心体验无关

现实判断：

V1 用户 = 你自己（或极少数）

“国内要开代理”在这个阶段 不是致命问题

工程建议：

llm/ 层直接定义一个 LLMClient 接口

当前实现是 OpenAIClientDirect

未来再加 OpenAIClientViaBackend 即可

➡️ 结论：V1 用 A，不纠结

2. 「可选前后文」V1 做不做
✅ V1 建议：不做前后文，只传选中文本

理由：

Accessibility 在不同 App / WebView / 自定义控件下
“前后文能不能稳定拿到”高度不确定

你现在最怕的是：

“产品体验还没跑顺，就被边缘情况拖死”

而且从你的产品理念看：

你是要 “解释一句话”

不是做语义理解系统

未来演化成本：

后续加前后文 = 只改 prompt + Accessibility 抽取逻辑

不影响 UI / 主流程

➡️ 结论：V1 不含前后文，写死这一点

README / TechDoc 里可以明确写：
“V1 only operates on explicitly selected text.”

3. API Key / 配置与安全
分情况给你最实用的建议
✅ V1（自用 / 原型）建议：

Key 由用户在 App 内输入

使用：

Android Keystore

或 EncryptedSharedPreferences

不写死在代码里

不提交到仓库

为什么：

比“写死 debug key”稍微多 10 分钟

但避免未来一切尴尬

不建议：

❌ 写在 BuildConfig

❌ 写在 repo 里哪怕是 private（你以后一定会忘）

➡️ 结论：V1 = 用户输入 + 本地加密存储

4. Android 版本与权限
✅ 推荐配置

minSdk = 24（Android 7.0）

targetSdk = 34

这是一个非常稳的选择：

覆盖面足够

Accessibility / Overlay API 都成熟

不被新系统限制卡死

Accessibility 引导
✅ V1 建议：做最小引导

不是新手教学，只做这 2 件事：

App 首页检测：

无障碍未开启 → 显示说明卡片

一个按钮：

跳转到系统无障碍设置页

不需要动画、不需要图解。

➡️ 结论：V1 要有“去开启无障碍”的引导，但极简

5. 浮窗与按钮 UI 粒度
✅ V1 建议：先可用，不设计

不追 Material

不调字号体系

不纠结圆角

只满足：

看得清

点得到

关得掉

你现在的最大风险不是“丑”，而是：

还没验证体验，就把时间耗在样式上

➡️ 结论：V1 只做 functional UI

6. 包名
✅ 现在就定（非常对）

建议直接用你会长期用的：

com.inlinenote.android





原则：

不用 demo 名

不用 temp

不用 test

改包名在 Android 里是真的痛苦，你这个提醒非常专业。

➡️ 结论：现在定，别改

7. 错误与网络处理
✅ V1 建议（克制版）

请求失败时：

浮窗内显示错误提示

文案要“像人说话”，例如：

网络不可用，未能生成解释

提供：

一个「关闭」按钮

不做重试按钮

为什么？

重试 = 状态管理 + 再一次请求

对 V1 没有决定性价值

➡️ 结论：V1 只保证“失败时不沉默”