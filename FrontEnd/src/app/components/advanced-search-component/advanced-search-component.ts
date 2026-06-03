import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgForOf, NgIf } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { HttpClient, HttpParams } from '@angular/common/http';

interface PostSearchResponse {
    posts: Post[];
    total: number;
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
  selector: 'app-advanced-search-component',
  standalone: true,
  imports: [FormsModule, NgIf, NgForOf],
  templateUrl: './advanced-search-component.html',
  styleUrl: './advanced-search-component.scss'
})
export class AdvancedSearchComponent implements OnInit {
  query = '';
  
  // Advanced search options
  selectedSource = '';
  startDate = '';
  endDate = '';
  searchInContent = false;
  
  // Available sources for dropdown - will be loaded from backend
  availableSources: string[] = [];
  isLoadingSources = false;

  // 分页状态
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;
  totalItems = 0;

  results: Post[] = [];
  hasSearched = false;
  isLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private sanitizer: DomSanitizer,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Load available sources from backend
    this.loadAvailableSources();
    
    // Check if there are query parameters from navigation
    this.route.queryParams.subscribe(params => {
      if (params['query']) {
        this.query = params['query'];
        // Optionally auto-search if query exists
        // this.onSearch();
      }
    });
  }

  loadAvailableSources() {
    this.isLoadingSources = true;
    this.http.get<string[]>('http://localhost:8080/posts/sources').subscribe({
      next: (sources) => {
        this.availableSources = sources;
        this.isLoadingSources = false;
      },
      error: (err) => {
        console.error('Failed to load post sources:', err);
        // Fallback to default sources if API fails
        this.availableSources = [
          '教务通知',
          '学术动态',
          '校园新闻',
          '学生工作',
          '科研通知'
        ];
        this.isLoadingSources = false;
      }
    });
  }

  onSearch() {
    if (!this.query.trim()) return;
    
    // Basic date validation
    if (this.startDate && this.endDate && this.startDate > this.endDate) {
      alert('开始日期不能晚于结束日期');
      return;
    }
    
    this.currentPage = 1;
    this.fetchData();
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.fetchData();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.fetchData();
    }
  }

  fetchData() {
    this.isLoading = true;
    this.hasSearched = true;

    // Determine the API endpoint and parameters based on search mode
    let apiEndpoint: string;
    let params = new HttpParams()
      .set('page', (this.currentPage - 1).toString())
      .set('size', this.pageSize.toString());
    
    if (this.searchInContent) {
      // If searching in content, use keyword endpoint
      apiEndpoint = 'http://localhost:8080/posts/search/keyword';
      params = params.set('keyword', this.query);
    } else {
      // Default to title search
      apiEndpoint = 'http://localhost:8080/posts/search/title';
      params = params.set('title', this.query);
    }
    
    // Add advanced search parameters if specified
    if (this.selectedSource) {
      params = params.set('source', this.selectedSource);
    }
    
    if (this.startDate) {
      params = params.set('startDate', this.startDate);
    }
    
    if (this.endDate) {
      params = params.set('endDate', this.endDate);
    }

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

  goBack() {
    this.router.navigate(['/search']);
  }
}
