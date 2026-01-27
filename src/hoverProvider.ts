/**
 * HoverProvider 实现
 * 整合行级解释和符号级解释，格式化输出
 */

import * as vscode from 'vscode';
import { parseLineSummary, scanTokens } from './parser';
import { loadSymbols, getSymbolInfo, isSymbolInfoV1, UnifiedSymbolInfo } from './symbolLoader';

/**
 * 创建 Hover 内容
 */
export function createHoverContent(
    document: vscode.TextDocument,
    position: vscode.Position,
    extensionPath: string
): vscode.Hover | null {
    const line = document.lineAt(position.line);
    const lineText = line.text;
    const lineSummary = parseLineSummary(lineText);
    const tokens = scanTokens(lineText);
    
    // 加载符号表
    const symbols = loadSymbols(extensionPath);
    
    // 收集符号解释（去重）
    const symbolExplanations = new Map<string, UnifiedSymbolInfo>();
    for (const { token } of tokens) {
        const symbolInfo = getSymbolInfo(token, symbols);
        if (symbolInfo && !symbolExplanations.has(token)) {
            symbolExplanations.set(token, symbolInfo);
        }
    }

    // 如果没有行级总结也没有符号解释，返回 null
    if (!lineSummary.type && symbolExplanations.size === 0) {
        return null;
    }

    // 构建 Markdown 内容
    const markdown = new vscode.MarkdownString();
    markdown.isTrusted = true; // 允许 Markdown 中的链接等

    // 标题
    markdown.appendMarkdown('### 🔰 初学者解释\n\n');

    // 行级解释
    if (lineSummary.type) {
        markdown.appendMarkdown('**【行解释】**\n\n');
        markdown.appendMarkdown(`${lineSummary.description}\n\n`);
    }

    // 符号解释
    if (symbolExplanations.size > 0) {
        markdown.appendMarkdown('**【符号解释】**\n\n');
        
        for (const [token, info] of symbolExplanations.entries()) {
            // 显示 token（转义特殊字符）
            const displayToken = token.replace(/\\/g, '\\\\').replace(/\*/g, '\\*');
            
            // V1 格式：显示术语和中文
            if (isSymbolInfoV1(info)) {
                markdown.appendMarkdown(`**\`${displayToken}\`** (${info.zh}, ${info.term})\n\n`);
                markdown.appendMarkdown(`${info.layer1}\n\n`);
                
                // Layer2（可展开）
                if (info.layer2) {
                    markdown.appendMarkdown('<details>\n');
                    markdown.appendMarkdown('<summary>展开更多</summary>\n\n');
                    
                    // 常见误解（not）
                    if (info.layer2.not && info.layer2.not.length > 0) {
                        markdown.appendMarkdown('**常见误解：**\n');
                        for (const notItem of info.layer2.not) {
                            markdown.appendMarkdown(`- ${notItem}\n`);
                        }
                        markdown.appendMarkdown('\n');
                    }
                    
                    // 对比示例（compare）
                    if (info.layer2.compare && info.layer2.compare.length > 0) {
                        markdown.appendMarkdown('**对比示例：**\n');
                        for (const compareItem of info.layer2.compare) {
                            markdown.appendMarkdown(`- \`${compareItem.code}\` → ${compareItem.note}\n`);
                        }
                        markdown.appendMarkdown('\n');
                    }
                    
                    markdown.appendMarkdown('</details>\n\n');
                }
                
                // 示例（examples）
                if (info.examples && info.examples.length > 0) {
                    markdown.appendMarkdown('**示例：** ');
                    const examplesText = info.examples.map(ex => `\`${ex}\``).join('、');
                    markdown.appendMarkdown(examplesText);
                    markdown.appendMarkdown('\n\n');
                }
            } else {
                // 旧格式（向后兼容）
                markdown.appendMarkdown(`**\`${displayToken}\`** ${info.layer1}\n\n`);
                
                if (info.layer2) {
                    markdown.appendMarkdown('<details>\n');
                    markdown.appendMarkdown('<summary>展开更多</summary>\n\n');
                    
                    if (info.layer2.clarification) {
                        markdown.appendMarkdown(`💡 ${info.layer2.clarification}\n\n`);
                    }
                    if (info.layer2.example) {
                        markdown.appendMarkdown(`📝 示例：\`${info.layer2.example}\`\n\n`);
                    }
                    if (info.layer2.casual) {
                        markdown.appendMarkdown(`💬 ${info.layer2.casual}\n\n`);
                    }
                    
                    markdown.appendMarkdown('</details>\n\n');
                }
            }
        }
    }

    // 返回 Hover 对象
    return new vscode.Hover(markdown, line.range);
}
