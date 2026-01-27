/**
 * 轻量级行文本解析器
 * 用于识别行级总结和符号 token
 */

export interface LineSummary {
    type: 'def' | 'return' | 'assignment' | 'print' | 'import' | null;
    description: string;
}

export interface TokenMatch {
    token: string;
    position: number;
}

/**
 * 解析行级总结（5类：def/return/assignment/print/import）
 */
export function parseLineSummary(line: string): LineSummary {
    const trimmed = line.trim();
    
    // 跳过空行和注释
    if (!trimmed || trimmed.startsWith('#')) {
        return { type: null, description: '' };
    }

    // 1. def - 定义函数
    const defMatch = trimmed.match(/^def\s+(\w+)\s*\(/);
    if (defMatch) {
        return {
            type: 'def',
            description: `定义函数 ${defMatch[1]}`
        };
    }

    // 2. return - 返回
    const returnMatch = trimmed.match(/^return\s+(.+)/);
    if (returnMatch) {
        return {
            type: 'return',
            description: `返回 ${returnMatch[1].trim()}`
        };
    }
    if (trimmed.match(/^return\s*$/)) {
        return {
            type: 'return',
            description: '返回 None（无返回值）'
        };
    }

    // 3. print - 打印
    const printMatch = trimmed.match(/print\s*\(/);
    if (printMatch) {
        return {
            type: 'print',
            description: '打印内容到控制台'
        };
    }

    // 4. import - 导入
    const importMatch = trimmed.match(/^import\s+(\w+)/);
    if (importMatch) {
        return {
            type: 'import',
            description: `导入模块 ${importMatch[1]}`
        };
    }
    const fromImportMatch = trimmed.match(/^from\s+(\S+)\s+import\s+(.+)/);
    if (fromImportMatch) {
        return {
            type: 'import',
            description: `从 ${fromImportMatch[1]} 导入 ${fromImportMatch[2].trim()}`
        };
    }

    // 5. assignment - 赋值（变量名 = 值）
    const assignmentMatch = trimmed.match(/^(\w+)\s*=\s*(.+)/);
    if (assignmentMatch) {
        return {
            type: 'assignment',
            description: `将 ${assignmentMatch[2].trim()} 赋给变量 ${assignmentMatch[1]}`
        };
    }

    return { type: null, description: '' };
}

/**
 * 扫描行中的符号 token
 * 返回所有匹配的符号及其位置
 */
export function scanTokens(line: string): TokenMatch[] {
    const tokens: TokenMatch[] = [];
    
    // 定义要匹配的符号（按长度从长到短排序，避免短符号覆盖长符号）
    const tokenPatterns = [
        // 转义字符（最长优先）
        { pattern: /\\n/g, token: '\\n' },
        { pattern: /\\t/g, token: '\\t' },
        { pattern: /\\"/g, token: '\\"' },
        { pattern: /\\'/g, token: "\\'" },
        { pattern: /\\\\/g, token: '\\\\' },
        // 复合运算符（最长优先）
        { pattern: /\*\*=/g, token: '**=' },
        { pattern: /\/\/=/g, token: '//=' },
        { pattern: /%=/g, token: '%=' },
        { pattern: /\*=/g, token: '*=' },
        { pattern: /\/=/g, token: '/=' },
        { pattern: /\*\*/g, token: '**' },
        { pattern: /\/\//g, token: '//' },
        { pattern: /<=/g, token: '<=' },
        { pattern: />=/g, token: '>=' },
        { pattern: /==/g, token: '==' },
        { pattern: /!=/g, token: '!=' },
        { pattern: /\+=/g, token: '+=' },
        { pattern: /-=/g, token: '-=' },
        // 关键字（使用单词边界）
        { pattern: /class\b/g, token: 'class' },
        { pattern: /def\b/g, token: 'def' },
        { pattern: /return\b/g, token: 'return' },
        { pattern: /import\b/g, token: 'import' },
        { pattern: /from\b/g, token: 'from' },
        { pattern: /as\b/g, token: 'as' },
        { pattern: /if\b/g, token: 'if' },
        { pattern: /elif\b/g, token: 'elif' },
        { pattern: /else\b/g, token: 'else' },
        { pattern: /for\b/g, token: 'for' },
        { pattern: /while\b/g, token: 'while' },
        { pattern: /break\b/g, token: 'break' },
        { pattern: /continue\b/g, token: 'continue' },
        { pattern: /with\b/g, token: 'with' },
        { pattern: /try\b/g, token: 'try' },
        { pattern: /except\b/g, token: 'except' },
        { pattern: /finally\b/g, token: 'finally' },
        { pattern: /raise\b/g, token: 'raise' },
        { pattern: /lambda\b/g, token: 'lambda' },
        { pattern: /pass\b/g, token: 'pass' },
        { pattern: /in\b/g, token: 'in' },
        { pattern: /is\b/g, token: 'is' },
        { pattern: /and\b/g, token: 'and' },
        { pattern: /or\b/g, token: 'or' },
        { pattern: /not\b/g, token: 'not' },
        { pattern: /True\b/g, token: 'True' },
        { pattern: /False\b/g, token: 'False' },
        { pattern: /None\b/g, token: 'None' },
        // 其他符号
        { pattern: /\.\.\./g, token: '...' },
        { pattern: /\(/g, token: '()' },  // 括号统一映射为 "()"
        { pattern: /\)/g, token: '()' },
        { pattern: /\[/g, token: '[]' },  // 方括号统一映射为 "[]"
        { pattern: /\]/g, token: '[]' },
        { pattern: /\{/g, token: '{' },
        { pattern: /\}/g, token: '}' },
        { pattern: /:/g, token: ':' },
        { pattern: /\./g, token: '.' },
        { pattern: /,/g, token: ',' },
        { pattern: /#/g, token: '#' },
        { pattern: /</g, token: '<' },
        { pattern: />/g, token: '>' },
        { pattern: /\+/g, token: '+' },
        { pattern: /-/g, token: '-' },
        { pattern: /\*/g, token: '*' },
        { pattern: /\//g, token: '/' },
        { pattern: /%/g, token: '%' },
        { pattern: /=/g, token: '=' },
    ];

    // 简单处理：跳过字符串内的符号（使用引号匹配）
    // 这是一个简化版本，不处理多行字符串和转义引号
    const stringPattern = /(['"])(?:(?=(\\?))\2.)*?\1/g;
    const stringRanges: Array<{ start: number; end: number }> = [];
    let match;
    
    while ((match = stringPattern.exec(line)) !== null) {
        stringRanges.push({ start: match.index, end: match.index + match[0].length });
    }

    // 检查位置是否在字符串内
    function isInString(pos: number): boolean {
        return stringRanges.some(range => pos >= range.start && pos < range.end);
    }

    // 扫描所有符号
    for (const { pattern, token } of tokenPatterns) {
        pattern.lastIndex = 0; // 重置正则
        while ((match = pattern.exec(line)) !== null) {
            const position = match.index;
            // 跳过字符串内的符号（但保留转义字符，因为它们在字符串内是有意义的）
            if (!isInString(position) || token.startsWith('\\')) {
                tokens.push({ token, position });
            }
        }
    }

    // 去重并排序（按位置）
    const uniqueTokens = new Map<string, number>();
    for (const { token, position } of tokens) {
        const key = `${token}:${position}`;
        if (!uniqueTokens.has(key)) {
            uniqueTokens.set(key, position);
        }
    }

    let result = Array.from(uniqueTokens.entries())
        .map(([key, position]) => {
            const token = key.split(':')[0];
            return { token, position };
        })
        .sort((a, b) => a.position - b.position);

    // 后处理：处理多词符号（"is not", "not in"）
    const processedResult: TokenMatch[] = [];
    for (let i = 0; i < result.length; i++) {
        const current = result[i];
        const next = result[i + 1];

        // 检查 "is not"
        if (current.token === 'is' && next && next.token === 'not' && 
            next.position === current.position + 3) { // "is " 是 3 个字符
            processedResult.push({ token: 'is not', position: current.position });
            i++; // 跳过下一个 token
            continue;
        }

        // 检查 "not in"
        if (current.token === 'not' && next && next.token === 'in' && 
            next.position === current.position + 4) { // "not " 是 4 个字符
            processedResult.push({ token: 'not in', position: current.position });
            i++; // 跳过下一个 token
            continue;
        }

        processedResult.push(current);
    }

    // 后处理：检查函数调用（"len(...)", "print(...)", "range(...)"）
    // 这些在 symbolsV1.json 中是特殊 token
    const functionCalls = ['len', 'print', 'range'];
    for (let i = 0; i < processedResult.length; i++) {
        const current = processedResult[i];
        if (functionCalls.includes(current.token)) {
            // 检查后面是否有括号
            const nextToken = processedResult.find(t => 
                t.position > current.position && 
                (t.token === '()' || t.token === '(')
            );
            if (nextToken && nextToken.position <= current.position + current.token.length + 1) {
                // 找到函数调用，添加特殊 token
                processedResult.push({ 
                    token: `${current.token}(...)`, 
                    position: current.position 
                });
            }
        }
    }

    return processedResult.sort((a, b) => a.position - b.position);
}
