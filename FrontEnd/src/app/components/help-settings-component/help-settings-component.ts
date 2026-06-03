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
  selector: 'app-help-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-settings-component.html',
  styleUrls: ['./help-settings-component.scss']
})
export class HelpSettingsComponent implements OnInit {
  content: HelpContent | null = null;
  loading = true;
  error = false;

  constructor(private helpService: HelpService) {}

  ngOnInit(): void {
    this.loadContent();
  }

  loadContent(): void {
    this.helpService.getModuleHelp('settings').subscribe({
      next: (data) => {
        this.content = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load settings help content:', err);
        this.error = true;
        this.loading = false;
        // Fallback content
        this.content = {
          title: '设置模块',
          description: '设置模块提供了各种系统和用户偏好的配置选项，帮助用户个性化使用体验。',
          features: [
            '隐私设置：控制个人信息的可见性',
            '账号安全：修改密码的安全选项'
          ],
          usage: [
            '访问设置页面（/settings）',
            '浏览不同的设置分类',
            '修改需要调整的设置项',
            '点击保存按钮应用更改',
            '根据需要可以随时返回修改设置'
          ]
        };
      }
    });
  }
}
