import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HelpService } from '../../services/help.service';

interface HelpContent {
  title: string;
  description: string;
  features: string[];
  usage: string[];
}

@Component({
  selector: 'app-help-ai',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-ai-component.html',
  styleUrls: ['./help-ai-component.scss']
})
export class HelpAiComponent implements OnInit {
  content: HelpContent | null = null;
  loading = true;
  error = false;

  constructor(private helpService: HelpService) {}

  ngOnInit(): void {
    this.loadContent();
  }

  loadContent(): void {
    this.helpService.getModuleHelp('ai').subscribe({
      next: (data) => {
        this.content = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load AI help content:', err);
        this.error = true;
        this.loading = false;
        // Fallback content
        this.content = {
          title: 'AI模块',
          description: 'SSPUBot集成了先进的AI聊天功能，可以与智能机器人进行对话，获取帮助和信息。',
          features: [
            '智能对话：与AI机器人进行自然语言对话',
            '多机器人支持：可选择不同的AI机器人进行交互',
            '上下文理解：AI能够理解对话上下文，提供连贯的回复',
            '个性化配置：管理员可配置不同的AI机器人及其参数'
          ],
          usage: [
            '访问AI聊天页面（/chat）',
            '选择要使用的AI机器人（如果有多个可选）',
            '在输入框中输入您的问题或消息',
            '点击发送按钮或按Enter键发送消息',
            '查看AI的回复并继续对话'
          ]
        };
      }
    });
  }
}
