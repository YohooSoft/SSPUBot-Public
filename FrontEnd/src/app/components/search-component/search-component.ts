import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgForOf, NgIf } from '@angular/common';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { HttpClient, HttpParams } from '@angular/common/http'; // 确保引入 HttpParams

// 对应后端的 DTO，注意大小写要和后端 JSON 一致
// 假设后端返回: { "posts": [...], "total": 100 } (Java默认小写)
// 或者 { "Posts": [...], "Total": 100 } (如果你做了特殊处理)
// 这里为了保险，我们将接口定义稍微灵活一点，或者你在 http 请求中做 map
interface PostSearchResponse {
    posts: Post[]; // 对应 Java: List<Post> posts
    total: number; // 对应 Java: long total
}

export interface Post {
  id?: number;
  postName?: string;
  postReleaseTime?: string;
  postSource?: string;
  postContent?: string;
  postUrl?: string;
  postFiles?: string;
  postContentUsingMarkdown?: string;
  postSimplifiedContent?: string;
}

@Component({
  selector: 'app-search-component',
  standalone: true,
  imports: [FormsModule, NgIf, NgForOf],
  templateUrl: './search-component.html',
  styleUrl: './search-component.scss'
})
export class SearchComponent implements OnInit {
  query = '';

  // 分页状态
  currentPage = 1; // UI显示用（从1开始）
  pageSize = 10;
  totalPages = 0;
  totalItems = 0;

  results: Post[] = [];
  hasSearched = false;
  isLoading = false; // 加载状态

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private sanitizer: DomSanitizer,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  navigateToAdvancedSearch() {
    this.router.navigate(['/advancedSearch'], {
      queryParams: { query: this.query }
    });
  }

  // 1. 点击搜索按钮（重置页码为1）
  onSearch() {
    if (!this.query.trim()) return;
    this.currentPage = 1;
    this.fetchData();
  }

  // 2. 上一页
  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.fetchData();
    }
  }

  // 3. 下一页
  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.fetchData();
    }
  }

  // 4. 核心数据请求方法
  fetchData() {
    this.isLoading = true;
    this.hasSearched = true;

    // Use title search endpoint
    const apiEndpoint = '/api/posts/search/title';
    
    // Construct parameters
    const params = new HttpParams()
      .set('title', this.query)
      .set('page', (this.currentPage - 1).toString())
      .set('size', this.pageSize.toString());

    this.http.get<PostSearchResponse>(apiEndpoint, { params }).subscribe({
      next: (res) => {
        this.results = res.posts || [];
        this.totalItems = res.total || 0;
        this.totalPages = Math.ceil(this.totalItems / this.pageSize);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('搜索失败', err);
        this.results = [];
        this.totalItems = 0;
        this.totalPages = 0;
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  getHighlightedText(text: string | undefined): SafeHtml {
    if (!text) return '';
    if (!this.query || !this.query.trim()) return text;
    const safeQuery = this.query.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    const regex = new RegExp(`(${safeQuery})`, 'gi');
    const replaced = text.replace(regex, '<span class="highlight">$1</span>');
    return this.sanitizer.bypassSecurityTrustHtml(replaced);
  }

  ngOnInit(): void {
  }
}
