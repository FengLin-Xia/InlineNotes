/**
 * 符号表加载器
 * 从 symbols.json 或 symbolsV1.json 加载符号解释
 * 优先使用 symbolsV1.json（新格式），回退到 symbols.json（旧格式）
 */

import * as fs from 'fs';
import * as path from 'path';

// 新格式（symbolsV1.json）的接口
export interface SymbolInfoV1 {
    term: string;
    zh: string;
    layer1: string;
    layer2?: {
        not?: string[];
        compare?: Array<{ code: string; note: string }>;
    };
    examples?: string[];
}

export interface SymbolsDataV1 {
    version: number;
    language: string;
    tokens: Record<string, SymbolInfoV1>;
}

// 旧格式（symbols.json）的接口
export interface SymbolInfo {
    layer1: string;
    layer2?: {
        clarification?: string;
        example?: string;
        casual?: string;
    };
}

export interface SymbolsData {
    tokens: Record<string, SymbolInfo>;
}

// 统一的内部格式
export interface UnifiedSymbolInfo {
    term?: string;
    zh?: string;
    layer1: string;
    layer2?: {
        clarification?: string;
        example?: string;
        casual?: string;
        not?: string[];
        compare?: Array<{ code: string; note: string }>;
    };
    examples?: string[];
}

export interface UnifiedSymbolsData {
    tokens: Record<string, UnifiedSymbolInfo>;
}

let symbolsCache: UnifiedSymbolsData | null = null;

/**
 * 转换新格式到统一格式
 */
function convertV1ToUnified(v1Data: SymbolsDataV1): UnifiedSymbolsData {
    const unified: UnifiedSymbolsData = { tokens: {} };
    
    for (const [token, info] of Object.entries(v1Data.tokens)) {
        unified.tokens[token] = {
            term: info.term,
            zh: info.zh,
            layer1: info.layer1,
            layer2: info.layer2 ? {
                not: info.layer2.not,
                compare: info.layer2.compare,
            } : undefined,
            examples: info.examples,
        };
    }
    
    return unified;
}

/**
 * 转换旧格式到统一格式
 */
function convertOldToUnified(oldData: SymbolsData): UnifiedSymbolsData {
    const unified: UnifiedSymbolsData = { tokens: {} };
    
    for (const [token, info] of Object.entries(oldData.tokens)) {
        unified.tokens[token] = {
            layer1: info.layer1,
            layer2: info.layer2,
        };
    }
    
    return unified;
}

/**
 * 加载符号表
 * 优先加载 symbolsV1.json，如果不存在则加载 symbols.json
 */
export function loadSymbols(contextPath: string): UnifiedSymbolsData {
    if (symbolsCache) {
        return symbolsCache;
    }

    // 优先尝试加载 symbolsV1.json（新格式）
    const v1Paths = [
        path.join(contextPath, 'src', 'symbolsV1.json'),
        path.join(contextPath, 'out', 'symbolsV1.json'),
        path.join(__dirname, 'symbolsV1.json'),
    ];

    for (const symbolsPath of v1Paths) {
        try {
            if (fs.existsSync(symbolsPath)) {
                const content = fs.readFileSync(symbolsPath, 'utf-8');
                const v1Data = JSON.parse(content) as SymbolsDataV1;
                symbolsCache = convertV1ToUnified(v1Data);
                console.log(`已加载 symbolsV1.json: ${symbolsPath}`);
                return symbolsCache;
            }
        } catch (error) {
            console.warn(`加载 symbolsV1.json 失败 (${symbolsPath}):`, error);
        }
    }

    // 回退到 symbols.json（旧格式）
    const oldPaths = [
        path.join(contextPath, 'src', 'symbols.json'),
        path.join(contextPath, 'out', 'symbols.json'),
        path.join(__dirname, 'symbols.json'),
    ];

    for (const symbolsPath of oldPaths) {
        try {
            if (fs.existsSync(symbolsPath)) {
                const content = fs.readFileSync(symbolsPath, 'utf-8');
                const oldData = JSON.parse(content) as SymbolsData;
                symbolsCache = convertOldToUnified(oldData);
                console.log(`已加载 symbols.json: ${symbolsPath}`);
                return symbolsCache;
            }
        } catch (error) {
            console.warn(`加载 symbols.json 失败 (${symbolsPath}):`, error);
        }
    }

    console.error('加载符号表失败: 未找到 symbolsV1.json 或 symbols.json');
    return { tokens: {} };
}

/**
 * 获取符号解释
 */
export function getSymbolInfo(symbol: string, symbols: UnifiedSymbolsData): UnifiedSymbolInfo | null {
    return symbols.tokens[symbol] || null;
}

/**
 * 判断符号信息是否为 V1 格式（有 term 和 zh 字段）
 */
export function isSymbolInfoV1(info: UnifiedSymbolInfo): boolean {
    return info.term !== undefined && info.zh !== undefined;
}
