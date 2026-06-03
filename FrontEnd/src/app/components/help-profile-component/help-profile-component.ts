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
  selector: 'app-help-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-profile-component.html',
  styleUrls: ['./help-profile-component.scss']
})
export class HelpProfileComponent implements OnInit {
  content: HelpContent | null = null;
  loading = true;
  error = false;

  constructor(private helpService: HelpService) {}

  ngOnInit(): void {
    this.loadContent();
  }

  loadContent(): void {
    this.helpService.getModuleHelp('profile').subscribe({
      next: (data) => {
        this.content = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load profile help content:', err);
        this.error = true;
        this.loading = false;
        // Fallback content
        this.content = {
          title: '个人资料模块',
          description: '个人资料模块允许用户查看和编辑自己的个人信息，管理账号设置。',
          features: [
            '查看个人信息：显示名称、出生年月等信息',
            '编辑资料：修改显示名称、出生年月等信息',
          ],
          usage: [
            '访问个人资料页面（/profile）',
            '点击编辑按钮进入编辑模式',
            '修改需要更新的信息',
            '点击保存按钮保存更改',
            '查看更新后的个人资料'
          ]
        };
      }
    });
  }
}
