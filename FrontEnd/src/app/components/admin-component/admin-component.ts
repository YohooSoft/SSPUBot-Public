import {Component, OnInit, OnDestroy, ChangeDetectorRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';

interface User {
  id: number;
  username: string;
  displayName: string;
  email: string;
  role: string;
  status: number;
  createdAt: string;
  avatarUrl?: string;
}

interface Bot {
  id: number;
  name: string;
  description: string;
  avatarUrl: string;
  systemPrompt: string;
  selectedModel: string;
  apiKey: string;
  baseUrl: string;
  isActive: boolean;
  isDefault?: boolean;
  temperature?: number;
  topK?: number;
  createdAt: string;
  updatedAt: string;
}

interface Spider {
  id: number;
  name: string;
  description: string;
  spiderClass: string;
  startUrls: string;
  allowedDomains: string;
  isActive: boolean;
  status: string;
  runtimeSeconds: number;  // Changed from progress
  startedAt?: string;  // Added for client-side timer calculation
  lastError?: string;
  lastRunTime?: string;
  createdAt: string;
  updatedAt: string;
}

interface Synonym {
  id: number;
  word: string;
  synonyms: string;
  category?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

@Component({
  selector: 'app-admin-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-component.html',
  styleUrls: ['./admin-component.scss']
})
export class AdminComponent implements OnInit, OnDestroy {
  activeTab: 'users' | 'bots' | 'spiders' | 'synonyms' = 'users';

  // Users
  users: User[] = [];
  currentUser: any = null;

  // Bots
  bots: Bot[] = [];
  showBotModal = false;
  editingBot: Bot | null = null;
  botForm: Partial<Bot> = {};

  // Spiders
  spiders: Spider[] = [];
  showSpiderModal = false;
  editingSpider: Spider | null = null;
  spiderForm: Partial<Spider> = {};
  openSpiderDropdownName: string | null = null;  // Changed from ID to name

  // Synonyms
  synonyms: Synonym[] = [];
  showSynonymModal = false;
  editingSynonym: Synonym | null = null;
  synonymForm: Partial<Synonym> = {};

  // Timer for runtime updates
  private runtimeUpdateInterval: any = null;

  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Check if user is admin
    this.currentUser = this.authService.getCurrentUser();
    if (!this.currentUser || (this.currentUser.role !== 'ADMIN' && this.currentUser.role !== 'ROLE_ADMIN')) {
      alert('您没有管理员权限');
      this.router.navigate(['/']);
      return;
    }

    // Check for page query parameter
    this.route.queryParams.subscribe(params => {
      const page = params['page'];
      if (page === 'users' || page === 'bots' || page === 'spiders' || page === 'synonyms') {
        this.switchTab(page);
      } else {
        this.loadUsers();
      }
    });
  }

  ngOnDestroy(): void {
    // Clear the runtime update interval when component is destroyed
    if (this.runtimeUpdateInterval) {
      clearInterval(this.runtimeUpdateInterval);
    }
  }

  switchTab(tab: 'users' | 'bots' | 'spiders' | 'synonyms'): void {
    this.activeTab = tab;
    
    // Update URL query parameter
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: tab },
      queryParamsHandling: 'merge'
    });
    
    if (tab === 'users') {
      this.loadUsers();
    } else if (tab === 'bots') {
      this.loadBots();
    } else if (tab === 'spiders') {
      this.loadSpiders();
    } else if (tab === 'synonyms') {
      this.loadSynonyms();
    }
  }

  // User Management
  loadUsers(): void {
    this.adminService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load users:', error);
        alert('加载用户列表失败');
        this.cdr.markForCheck();
      }
    });
  }

  banUser(user: User): void {
    if (user.id === this.currentUser.id) {
      alert('不能封禁自己');
      return;
    }

    if (confirm(`确定要封禁用户 ${user.username} 吗？`)) {
      this.adminService.banUser(user.id).subscribe({
        next: () => {
          alert('用户已封禁');
          this.loadUsers();
        },
        error: (error) => {
          console.error('Failed to ban user:', error);
          alert('封禁用户失败');
        }
      });
    }
  }

  unbanUser(user: User): void {
    if (confirm(`确定要解除封禁用户 ${user.username} 吗？`)) {
      this.adminService.unbanUser(user.id).subscribe({
        next: () => {
          alert('已解除封禁');
          this.loadUsers();
        },
        error: (error) => {
          console.error('Failed to unban user:', error);
          alert('解除封禁失败');
        }
      });
    }
  }

  getUserStatusText(status: number): string {
    switch (status) {
      case 0: return '未激活';
      case 1: return '正常';
      case 2: return '已封禁';
      case 3: return '已禁言';
      default: return '未知';
    }
  }

  // Bot Management
  loadBots(): void {
    this.adminService.getAllBots().subscribe({
      next: (bots) => {
        this.bots = bots;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load bots:', error);
        alert('加载机器人列表失败');
        this.cdr.markForCheck();
      }
    });
  }

  openBotModal(bot?: Bot): void {
    this.editingBot = bot || null;
    this.botForm = bot ? { ...bot } : {
      isActive: true,
      temperature: 0.7,
      topK: 40
    };
    this.showBotModal = true;
  }

  closeBotModal(): void {
    this.showBotModal = false;
    this.editingBot = null;
    this.botForm = {};
  }

  saveBot(): void {
    if (!this.botForm.name || !this.botForm.systemPrompt) {
      alert('请填写必填项：名称和系统提示词');
      return;
    }

    if (this.editingBot) {
      // Update existing bot
      this.adminService.updateBot(this.editingBot.id, this.botForm as Bot).subscribe({
        next: () => {
          alert('机器人已更新');
          this.closeBotModal();
          this.loadBots();
        },
        error: (error) => {
          console.error('Failed to update bot:', error);
          alert('更新机器人失败');
        }
      });
    } else {
      // Create new bot
      this.adminService.createBot(this.botForm as Bot).subscribe({
        next: () => {
          alert('机器人已创建');
          this.closeBotModal();
          this.loadBots();
        },
        error: (error) => {
          console.error('Failed to create bot:', error);
          alert('创建机器人失败');
        }
      });
    }
  }

  deleteBot(bot: Bot): void {
    if (confirm(`确定要删除机器人 ${bot.name} 吗？`)) {
      this.adminService.deleteBot(bot.id).subscribe({
        next: () => {
          alert('机器人已删除');
          this.loadBots();
        },
        error: (error) => {
          console.error('Failed to delete bot:', error);
          alert('删除机器人失败');
        }
      });
    }
  }

  setDefaultBot(bot: Bot): void {
    if (confirm(`确定要将 ${bot.name} 设置为默认机器人吗？\n\n默认机器人将在用户打开聊天页面时自动选中。`)) {
      // First, unset all other bots as default
      const updatePromises: Promise<any>[] = [];

      this.bots.forEach(b => {
        if (b.id !== bot.id && b.isDefault) {
          const updatedBot = { ...b, isDefault: false };
          updatePromises.push(
            this.adminService.updateBot(b.id, updatedBot).toPromise()
          );
        }
      });

      // Wait for all unset operations to complete, then set the new default
      Promise.all(updatePromises).then(() => {
        const updatedBot = { ...bot, isDefault: true };
        this.adminService.updateBot(bot.id, updatedBot).subscribe({
          next: () => {
            alert(`${bot.name} 已设置为默认机器人`);
            this.loadBots();
          },
          error: (error) => {
            console.error('Failed to set default bot:', error);
            alert('设置默认机器人失败');
          }
        });
      }).catch((error) => {
        console.error('Failed to unset other default bots:', error);
        alert('设置默认机器人失败');
      });
    }
  }

  // Spider Management
  loadSpiders(): void {
    this.adminService.getAllSpiders().subscribe({
      next: (spiders) => {
        this.spiders = spiders;
        this.startRuntimeUpdates();
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load spiders:', error);
        alert('加载爬虫列表失败');
        this.cdr.markForCheck();
      }
    });
  }

  startRuntimeUpdates(): void {
    // Clear any existing interval
    if (this.runtimeUpdateInterval) {
      clearInterval(this.runtimeUpdateInterval);
    }

    // Update runtime every second for running spiders
    this.runtimeUpdateInterval = setInterval(() => {
      this.spiders.forEach(spider => {
        if (spider.status === 'running' && spider.startedAt) {
          // Calculate runtime: current time - started_at
          const startTime = new Date(spider.startedAt).getTime();
          const currentTime = new Date().getTime();
          spider.runtimeSeconds = Math.floor((currentTime - startTime) / 1000);
        }
      });
    }, 1000); // Update every second
  }

  openSpiderModal(spider?: Spider): void {
    this.editingSpider = spider || null;
    this.spiderForm = spider ? { ...spider } : {
      isActive: true,
      status: 'idle',
      runtimeSeconds: 0
    };
    this.showSpiderModal = true;
  }

  closeSpiderModal(): void {
    this.showSpiderModal = false;
    this.editingSpider = null;
    this.spiderForm = {};
  }

  saveSpider(): void {
    if (!this.spiderForm.name || !this.spiderForm.spiderClass) {
      alert('请填写必填项：名称和爬虫类名');
      return;
    }

    if (this.editingSpider) {
      // Update existing spider
      this.adminService.updateSpider(this.editingSpider.id, this.spiderForm as Spider).subscribe({
        next: () => {
          alert('爬虫已更新');
          this.closeSpiderModal();
          this.loadSpiders();
        },
        error: (error) => {
          console.error('Failed to update spider:', error);
          alert('更新爬虫失败');
        }
      });
    } else {
      // Create new spider
      this.adminService.createSpider(this.spiderForm as Spider).subscribe({
        next: () => {
          alert('爬虫已创建');
          this.closeSpiderModal();
          this.loadSpiders();
        },
        error: (error) => {
          console.error('Failed to create spider:', error);
          alert('创建爬虫失败');
        }
      });
    }
  }

  deleteSpider(spider: Spider): void {
    if (confirm(`确定要删除爬虫 ${spider.name} 吗？`)) {
      this.adminService.deleteSpider(spider.id).subscribe({
        next: () => {
          alert('爬虫已删除');
          this.loadSpiders();
        },
        error: (error) => {
          console.error('Failed to delete spider:', error);
          alert('删除爬虫失败');
        }
      });
    }
  }

  startSpider(spider: Spider): void {
    if (spider.status === 'running') {
      alert('爬虫正在运行中');
      return;
    }

    this.adminService.startSpider(spider.id).subscribe({
      next: () => {
        alert('爬虫已启动');
        this.loadSpiders();
      },
      error: (error) => {
        console.error('Failed to start spider:', error);
        alert('启动爬虫失败');
      }
    });
  }

  stopSpider(spider: Spider): void {
    if (spider.status !== 'running') {
      alert('爬虫未在运行中');
      return;
    }

    if (confirm(`确定要停止爬虫 ${spider.name} 吗？`)) {
      this.adminService.stopSpider(spider.id).subscribe({
        next: () => {
          alert('爬虫已停止');
          this.loadSpiders();
        },
        error: (error) => {
          console.error('Failed to stop spider:', error);
          alert('停止爬虫失败');
        }
      });
    }
  }

  viewSpiderProgress(spider: Spider): void {
    this.adminService.getSpiderProgress(spider.name).subscribe({
      next: (progress) => {
        const runtimeText = this.formatRuntime(progress.runtimeSeconds || 0);
        const message = `
爬虫: ${progress.name}
状态: ${this.getSpiderStatusText(progress.status)}
运行时间: ${runtimeText}
最后运行: ${progress.lastRunTime || '未运行'}
${progress.lastError ? '错误: ' + progress.lastError : ''}
        `;
        alert(message);
      },
      error: (error) => {
        console.error('Failed to get spider progress:', error);
        alert('获取爬虫运行信息失败');
      }
    });
  }

  formatRuntime(seconds: number): string {
      const s = Number.isFinite(seconds) ? Math.max(0, Math.floor(seconds)) : 0;
      if (s === 0) return '0秒';

      const hours = Math.floor(s / 3600);
      const minutes = Math.floor((s % 3600) / 60);
      const secs = s % 60;

      const parts: string[] = [];
      if (hours > 0) parts.push(`${hours}小时`);
      if (minutes > 0) parts.push(`${minutes}分`);
      if (secs > 0 || parts.length === 0) parts.push(`${secs}秒`);

      return parts.join('');
    }


  toggleSpiderDropdown(spiderName: string): void {
    if (this.openSpiderDropdownName === spiderName) {
      this.openSpiderDropdownName = null;
    } else {
      this.openSpiderDropdownName = spiderName;
    }
  }

  isSpiderDropdownOpen(spiderName: string): boolean {
    return this.openSpiderDropdownName === spiderName;
  }

  closeSpiderDropdown(): void {
    this.openSpiderDropdownName = null;
  }

  getSpiderStatusText(status: string): string {
    switch (status) {
      case 'idle': return '空闲';
      case 'running': return '运行中';
      case 'stopped': return '已停止';
      case 'error': return '错误';
      default: return status;
    }
  }

  private parseLastRunTime(lastRunTime: string | undefined): Date | null {
    if (!lastRunTime) {
      return null;
    }

    try {
      const date = new Date(lastRunTime);
      if (isNaN(date.getTime())) {
        return null;
      }
      return date;
    } catch (e) {
      return null;
    }
  }

  formatLastRunTimeShort(lastRunTime: string | undefined): string {
    const date = this.parseLastRunTime(lastRunTime);
    if (!date) {
      return lastRunTime || '未运行';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  formatLastRunTimeFull(lastRunTime: string | undefined): string {
    const date = this.parseLastRunTime(lastRunTime);
    if (!date) {
      return lastRunTime || '未运行';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  truncateDescription(description: string | undefined, maxLength: number = 30): string {
    if (!description) {
      return '';
    }
    if (description.length <= maxLength) {
      return description;
    }
    return description.substring(0, maxLength) + '...';
  }

  // Synonym Management
  loadSynonyms(): void {
    this.adminService.getAllSynonyms().subscribe({
      next: (synonyms) => {
        this.synonyms = synonyms;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load synonyms:', error);
        alert('加载同义词列表失败');
        this.cdr.markForCheck();
      }
    });
  }

  openSynonymModal(synonym?: Synonym): void {
    this.editingSynonym = synonym || null;
    this.synonymForm = synonym ? { ...synonym } : {};
    this.showSynonymModal = true;
  }

  closeSynonymModal(): void {
    this.showSynonymModal = false;
    this.editingSynonym = null;
    this.synonymForm = {};
  }

  saveSynonym(): void {
    if (!this.synonymForm.word || !this.synonymForm.synonyms) {
      alert('请填写必填项：主词和同义词列表');
      return;
    }

    if (this.editingSynonym) {
      // Update existing synonym
      this.adminService.updateSynonym(this.editingSynonym.id, this.synonymForm as Synonym).subscribe({
        next: () => {
          alert('同义词已更新');
          this.closeSynonymModal();
          this.loadSynonyms();
        },
        error: (error) => {
          console.error('Failed to update synonym:', error);
          alert('更新同义词失败: ' + (error.error || error.message));
        }
      });
    } else {
      // Create new synonym
      this.adminService.createSynonym(this.synonymForm as Synonym).subscribe({
        next: () => {
          alert('同义词已创建');
          this.closeSynonymModal();
          this.loadSynonyms();
        },
        error: (error) => {
          console.error('Failed to create synonym:', error);
          alert('创建同义词失败: ' + (error.error || error.message));
        }
      });
    }
  }

  deleteSynonym(synonym: Synonym): void {
    if (confirm(`确定要删除同义词 "${synonym.word}" 吗？`)) {
      this.adminService.deleteSynonym(synonym.id).subscribe({
        next: () => {
          alert('同义词已删除');
          this.loadSynonyms();
        },
        error: (error) => {
          console.error('Failed to delete synonym:', error);
          alert('删除同义词失败');
        }
      });
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN');
  }

  truncateText(text: string | undefined, maxLength: number): string {
    if (!text) return '-';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }
}
