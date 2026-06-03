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
  selector: 'app-help-query',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './help-query-component.html',
  styleUrls: ['./help-query-component.scss']
})
export class HelpQueryComponent implements OnInit {
  content: HelpContent | null = null;
  loading = true;
  error = false;

  constructor(private helpService: HelpService) {}

  ngOnInit(): void {
    this.loadContent();
  }

  loadContent(): void {
    this.helpService.getModuleHelp('query').subscribe({
      next: (data) => {
        this.content = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load query help content:', err);
        this.error = true;
        this.loading = false;
        // Fallback content
        this.content = {
          title: '查询模块',
          description: 'SSPUBot提供强大的查询功能，帮助您快速找到所需的信息。',
          features: [
            '字符查询：查询单个汉字的详细信息，包括读音、释义、部首等',
            '词组查询：查询词组的含义、用法和相关例句',
            '例句查询：查找包含特定字词的例句',
            '高级搜索：支持多条件组合搜索，满足复杂查询需求'
          ],
          usage: [
            '在首页搜索框中输入要查询的内容',
            '选择查询类型（字符、词组或例句）',
            '点击搜索按钮或按Enter键',
            '查看搜索结果并浏览详细信息'
          ]
        };
      }
    });
  }
}
