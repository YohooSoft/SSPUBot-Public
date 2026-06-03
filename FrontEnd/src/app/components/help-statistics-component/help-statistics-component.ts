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
  selector: 'app-help-statistics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-statistics-component.html',
  styleUrls: ['./help-statistics-component.scss']
})
export class HelpStatisticsComponent implements OnInit {
  content: HelpContent | null = null;
  loading = true;
  error = false;

  constructor(private helpService: HelpService) {}

  ngOnInit(): void {
    this.loadContent();
  }

  loadContent(): void {
    this.helpService.getModuleHelp('statistics').subscribe({
      next: (data) => {
        this.content = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load statistics help content:', err);
        this.error = true;
        this.loading = false;
        // Fallback content
        this.content = {
          title: '统计模块',
          description: 'SSPUBot的统计模块提供了全面的数据分析功能，帮助您了解系统使用情况和数据趋势。',
          features: [
            '用户统计：查看用户注册、活跃度等数据',
            '查询统计：分析查询热度、频次等信息',
            '数据可视化：通过图表直观展示统计数据',
            '导出功能：支持将统计数据导出为文件'
          ],
          usage: [
            '访问统计页面（/statistics）',
            '选择要查看的统计类型',
            '设置时间范围和筛选条件',
            '查看统计图表和详细数据'
          ]
        };
      }
    });
  }
}
