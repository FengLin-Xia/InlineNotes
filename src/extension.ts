import * as vscode from 'vscode';
import { createHoverContent } from './hoverProvider';
import * as path from 'path';

export function activate(context: vscode.ExtensionContext) {
    console.log('Inline Notes 插件已激活');

    const extensionPath = context.extensionPath;

    // 注册 HoverProvider
    const hoverProvider = vscode.languages.registerHoverProvider('python', {
        provideHover(document: vscode.TextDocument, position: vscode.Position, token: vscode.CancellationToken) {
            return createHoverContent(document, position, extensionPath);
        }
    });

    context.subscriptions.push(hoverProvider);
}

export function deactivate() {
    console.log('Inline Notes 插件已停用');
}
