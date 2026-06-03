import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MemoryService, Memory, Bot } from '../../services/memory.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-bot-memory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bot-memory-component.html',
  styleUrls: ['./bot-memory-component.scss']
})
export class BotMemoryComponent implements OnInit {
  memories: Memory[] = [];
  bots: Bot[] = [];
  loading = true;
  saving = false;
  generatingBotIds: Set<number> = new Set(); // Track which bots are generating
  editingBotId: number | null = null;
  editingContent: string = '';
  showDeleteConfirm = false;
  deleteTargetBotId: number | null = null;
  errorMessage: string = '';
  successMessage: string = '';
  selectedBotId: number | null = null; // Track selected bot for left-right layout

  constructor(
    private memoryService: MemoryService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  /**
   * Select a bot to view/edit its memory
   */
  selectBot(botId: number): void {
    this.selectedBotId = botId;
    this.editingBotId = null;
    this.editingContent = '';
    this.cdr.markForCheck();
  }

  /**
   * Check if a bot is generating memory
   */
  isGenerating(botId: number): boolean {
    return this.generatingBotIds.has(botId);
  }

  /**
   * Load all bots and memories
   */
  loadData(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    // Load bots and memories in parallel
    Promise.all([
      firstValueFrom(this.memoryService.getAllBots()),
      firstValueFrom(this.memoryService.getAllMemories())
    ])
      .then(([bots, memories]) => {
        this.bots = bots || [];
        this.memories = memories || [];
        // Auto-select first bot if available
        if (this.bots.length > 0 && !this.selectedBotId) {
          this.selectedBotId = this.bots[0].id;
        }
        this.loading = false;
        this.cdr.markForCheck();
      })
      .catch(error => {
        console.error('Failed to load data:', error);
        this.errorMessage = '加载数据失败，请稍后重试';
        this.loading = false;
        this.cdr.markForCheck();
      });
  }

  /**
   * Get memory for a specific bot
   */
  getMemoryForBot(botId: number): Memory | undefined {
    return this.memories.find(m => m.botId === botId);
  }

  /**
   * Check if a bot has memory
   */
  hasMemory(botId: number): boolean {
    const memory = this.getMemoryForBot(botId);
    return !!memory && !!memory.content && memory.content.trim().length > 0;
  }

  /**
   * Start editing memory for a bot
   */
  startEdit(botId: number): void {
    const memory = this.getMemoryForBot(botId);
    this.editingBotId = botId;
    this.editingContent = memory?.content || '';
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();
  }

  /**
   * Cancel editing
   */
  cancelEdit(): void {
    this.editingBotId = null;
    this.editingContent = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();
  }

  /**
   * Save memory for a bot
   */
  saveMemory(botId: number): void {
    if (!this.editingContent || this.editingContent.trim().length === 0) {
      this.errorMessage = '记忆内容不能为空';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();

    this.memoryService.updateMemory(botId, this.editingContent)
      .subscribe({
        next: () => {
          this.successMessage = '记忆保存成功';
          this.editingBotId = null;
          this.editingContent = '';
          this.saving = false;
          this.loadData(); // Reload data to get updated memory
          this.cdr.markForCheck();
          
          // Clear success message after 3 seconds
          setTimeout(() => {
            this.successMessage = '';
            this.cdr.markForCheck();
          }, 3000);
        },
        error: (error) => {
          console.error('Failed to save memory:', error);
          this.errorMessage = '保存记忆失败，请稍后重试';
          this.saving = false;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Generate memory from chat history
   */
  generateMemory(botId: number): void {
    this.generatingBotIds.add(botId);
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();

    this.memoryService.generateMemory(botId)
      .subscribe({
        next: (response) => {
          const generatedMemory = response.generatedMemory || '';
          this.editingBotId = botId;
          this.editingContent = generatedMemory;
          this.successMessage = '已根据聊天记录生成记忆摘要';
          this.generatingBotIds.delete(botId);
          this.cdr.markForCheck();
          
          // Clear success message after 3 seconds
          setTimeout(() => {
            this.successMessage = '';
            this.cdr.markForCheck();
          }, 3000);
        },
        error: (error) => {
          console.error('Failed to generate memory:', error);
          this.errorMessage = error.error?.error || '生成记忆失败，请确保有聊天记录';
          this.generatingBotIds.delete(botId);
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Show delete confirmation dialog
   */
  confirmDelete(botId: number): void {
    this.deleteTargetBotId = botId;
    this.showDeleteConfirm = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.markForCheck();
  }

  /**
   * Cancel delete operation
   */
  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deleteTargetBotId = null;
    this.cdr.markForCheck();
  }

  /**
   * Delete memory for a bot
   */
  deleteMemory(botId: number): void {
    this.memoryService.deleteMemory(botId)
      .subscribe({
        next: () => {
          this.successMessage = '记忆已删除';
          this.showDeleteConfirm = false;
          this.deleteTargetBotId = null;
          this.loadData(); // Reload data to reflect deletion
          this.cdr.markForCheck();
          
          // Clear success message after 3 seconds
          setTimeout(() => {
            this.successMessage = '';
            this.cdr.markForCheck();
          }, 3000);
        },
        error: (error) => {
          console.error('Failed to delete memory:', error);
          this.errorMessage = '删除记忆失败，请稍后重试';
          this.showDeleteConfirm = false;
          this.deleteTargetBotId = null;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Navigate back to home
   */
  goBack(): void {
    this.router.navigate(['/']);
  }
}
