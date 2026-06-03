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
  selector: 'app-help-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-admin-component.html',
  styleUrls: ['./help-admin-component.scss']
})
export class HelpAdminComponent implements OnInit {
  content: HelpContent | null = null;
  loading = true;
  error = false;

  constructor(private helpService: HelpService) {}

  ngOnInit(): void {
    this.loadContent();
  }

  loadContent(): void {
    this.helpService.getModuleHelp('admin').subscribe({
      next: (data) => {
        this.content = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load admin help content:', err);
        this.error = true;
        this.loading = false;
        // Fallback content
        this.content = {
          title: '管理员模块',
          description: '管理员模块提供了系统管理的各项功能，仅限管理员用户访问。通过此模块，管理员可以管理用户、机器人、爬虫和同义词等系统资源。',
          features: [
            '用户管理：查看所有用户、封禁/解封用户账号',
            '机器人管理：添加、编辑、删除AI机器人，设置默认机器人',
            '爬虫管理：启动、停止爬虫任务，查看爬虫运行状态',
            '同义词管理：添加、编辑、删除同义词条目'
          ],
          usage: [
            '访问管理员页面（/admin）',
            '使用顶部标签页切换不同的管理功能',
            '在用户管理中，可以封禁或解封用户',
            '在机器人管理中，可以添加新机器人或编辑现有机器人配置',
            '在爬虫管理中，可以启动、停止爬虫并查看运行详情',
            '在同义词管理中，可以维护系统的同义词库'
          ]
        };
      }
    });
  }
}
