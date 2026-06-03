import {Component, ViewChild, ElementRef, AfterViewChecked, OnInit, inject, ChangeDetectorRef} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgForOf, NgIf } from '@angular/common';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { MarkdownComponentNew } from '../markdown-component/markdown-component';
import {Router} from '@angular/router';

// Interface for chat messages
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  datetime?: string; // Added for backend synchronization
  isMarkdown?: boolean; // Added to indicate if content should be rendered as markdown
}

// Interface for Bot
export interface Bot {
  id: number;
  name: string;
  description?: string;
  avatarUrl?: string;
  systemPrompt: string;
  selectedModel?: string;
  apiKey?: string;
  baseUrl?: string;
  isActive: boolean;
  isDefault?: boolean;
  createdAt: string;
  updatedAt: string;
}

// Interface for Memory
export interface Memory {
  botId: number;
  botName?: string;
  content: string;
  createdAt?: string;
  updatedAt?: string;
}

@Component({
  selector: 'app-ai-component',
  imports: [
    FormsModule,
    NgIf,
    NgForOf,
    DatePipe,
    MarkdownComponentNew
  ],
  templateUrl: './ai-component.html',
  styleUrl: './ai-component.scss'
})
export class AiComponent implements AfterViewChecked, OnInit {
  @ViewChild('chatMessages') private chatMessagesContainer?: ElementRef;

  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  messages: ChatMessage[] = [];
  userInput: string = '';
  isLoading: boolean = false;
  private shouldScrollToBottom = false;

  // Bot management
  bots: Bot[] = [];
  selectedBot: Bot | null = null;
  isLoadingBots: boolean = false;

  // Menu and context menu state
  showMoreMenu: boolean = false;
  showContextMenu: boolean = false;
  contextMenuX: number = 0;
  contextMenuY: number = 0;
  contextMenuMessage: ChatMessage | null = null;

  // Memory management
  showMemoryDialog: boolean = false;
  memories: Memory[] = [];
  selectedMemoryBotId: number | null = null;
  memoryContent: string = '';
  isLoadingMemories: boolean = false;

  // Configuration
  private readonly API_URL = 'http://localhost:8080';
  private readonly WELCOME_MESSAGE = '你好！请选择一个机器人开始对话。\n\nHello! Please select a bot to start chatting.';

  constructor() {
    // Add welcome message
    this.messages.push({
      id: this.generateId(),
      role: 'assistant',
      content: this.WELCOME_MESSAGE,
      timestamp: new Date()
    });
  }

  ngOnInit(): void {
    this.loadBots();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  // Load available bots
  async loadBots(): Promise<void> {
    this.isLoadingBots = true;
    try {
      const response = await this.http.get<Bot[]>(`${this.API_URL}/api/bots`).toPromise();
      this.bots = response || [];
      console.log('Loaded bots:', this.bots);

      // Auto-select default bot if available
      if (this.bots.length > 0 && !this.selectedBot) {
        const defaultBot = this.bots[0];

        if (defaultBot) {
          this.selectBot(defaultBot);
        }
      }
    } catch (error: any) {
      console.error('Error loading bots:', error);
      // Add error message to chat
      this.messages.push({
        id: this.generateId(),
        role: 'assistant',
        content: '加载机器人列表失败，请确保已登录。\n\nFailed to load bots. Please make sure you are logged in.',
        timestamp: new Date()
      });
      if(error.status === 403) {
          await this.router.navigate(['/login']);
      }
    } finally {
      this.isLoadingBots = false;
      this.shouldScrollToBottom = true;
      this.cdr.markForCheck();
    }
  }

  // Select a bot for chatting
  async selectBot(bot: Bot): Promise<void> {
    if (bot.id === this.selectedBot?.id) return;

    this.selectedBot = bot;

    // Clear previous messages except welcome
    this.messages = [{
      id: this.generateId(),
      role: 'assistant',
      content: `已切换到 ${bot.name}。\n\n${bot.description || '开始对话吧！'}\n\nSwitched to ${bot.name}. ${bot.description || 'Let\'s chat!'}`,
      timestamp: new Date()
    }];

    // Load chat history
    await this.loadChatHistory(bot.id);

    this.shouldScrollToBottom = true;
  }

  // Load chat history for the selected bot
  async loadChatHistory(botId: number): Promise<void> {
    try {
      const response = await this.http.get<any[]>(`${this.API_URL}/api/chat/history?botId=${botId}`).toPromise();

      if (response && response.length > 0) {
        // Clear welcome message and add history
        this.messages = response.map(msg => ({
          id: this.generateId(),
          role: msg.role as 'user' | 'assistant',
          content: msg.content,
          timestamp: new Date(msg.datetime || Date.now()),
          datetime: msg.datetime
        }));

        this.shouldScrollToBottom = true;

        this.cdr.markForCheck();
      }
    } catch (error) {
      console.error('Error loading chat history:', error);
      // Keep the welcome message if history loading fails
        this.cdr.markForCheck();
    }
  }

  // Send message handler
  async onSendMessage(): Promise<void> {
    if (!this.userInput.trim() || this.isLoading) {
      return;
    }

    if (!this.selectedBot) {
      alert('请先选择一个机器人！\nPlease select a bot first!');
      return;
    }

    // Add user message to chat
    const userMessage: ChatMessage = {
      id: this.generateId(),
      role: 'user',
      content: this.userInput,
      timestamp: new Date()
    };
    this.messages.push(userMessage);

    // Clear input
    const messageText = this.userInput;
    this.userInput = '';
    this.isLoading = true;

    try {
      // Call OpenRoute API
      const response = await this.http.post<any>(`${this.API_URL}/api/openroute/bot/chat`, {
        botId: this.selectedBot.id,
        message: messageText
      }).toPromise();
        this.cdr.markForCheck();
      // Add AI response to chat
      const aiMessage: ChatMessage = {
        id: this.generateId(),
        role: 'assistant',
        content: response.response || 'No response from bot',
        timestamp: new Date(),
        isMarkdown: response.responseFormat === 'markdown' // Check if response is markdown
      };
      this.messages.push(aiMessage);
      this.cdr.markForCheck();
    } catch (error) {
      console.error('Error sending message:', error);
      const errorMessage: ChatMessage = {
        id: this.generateId(),
        role: 'assistant',
        content: '抱歉，发生错误。请稍后再试。\n\nSorry, an error occurred. Please try again later.',
        timestamp: new Date()
      };
      this.messages.push(errorMessage);
    } finally {
      this.isLoading = false;
      this.shouldScrollToBottom = true;
      this.cdr.markForCheck();
    }
  }

  // Handle Enter key down
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.onSendMessage();
    }
  }

  // Generate unique ID for messages
  private generateId(): string {
    // Use crypto.randomUUID if available, fallback to timestamp + random
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `msg-${crypto.randomUUID()}`;
    }
    return `msg-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
  }

  // Scroll chat to bottom
  private scrollToBottom(): void {
    if (this.chatMessagesContainer) {
      const element = this.chatMessagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  // Toggle more menu
  toggleMoreMenu(): void {
    this.showMoreMenu = !this.showMoreMenu;
  }

  // Close more menu when clicking outside
  closeMoreMenu(): void {
    this.showMoreMenu = false;
  }

  // Clear all chat history
  async clearChatHistory(): Promise<void> {
    if (!this.selectedBot) return;

    if (!confirm('确定要清除所有聊天记录吗？\n\nAre you sure you want to clear all chat history?')) {
      return;
    }

    try {
      await this.http.delete(`${this.API_URL}/api/chat/history?botId=${this.selectedBot.id}`).toPromise();

      // Clear messages
      this.messages = [];

      this.shouldScrollToBottom = true;
      this.closeMoreMenu();
    } catch (error) {
      console.error('Error clearing chat history:', error);
      alert('清除聊天记录失败\n\nFailed to clear chat history');
    }
  }

  // Open memory management dialog
  openMemoryManagement(): void {
    this.showMemoryDialog = true;
    this.closeMoreMenu();
    this.loadAllMemories();
  }

  // Update memory for current bot
  async updateMemory(): Promise<void> {
    if (!this.selectedBot) return;

    this.closeMoreMenu();

    // Show loading indicator
    const confirmed = confirm('这将从您的聊天记录中生成记忆摘要。继续吗？\n\nThis will generate a memory summary from your chat history. Continue?');
    if (!confirmed) return;

    try {
      // Call backend to generate memory from chat history
      const response = await this.http.post<any>(`${this.API_URL}/api/memory/generate`, {
        botId: this.selectedBot.id
      }).toPromise();

      // Show the generated memory to user and ask if they want to save it
      const generatedMemory = response.generatedMemory;

      // Save the memory
      await this.http.post(`${this.API_URL}/api/memory`, {
        botId: this.selectedBot.id,
        content: generatedMemory
      }).toPromise();

      alert('记忆已更新\n\nMemory updated');
    } catch (error: any) {
      console.error('Error updating memory:', error);
      if (error.error && error.error.error) {
        alert('生成记忆失败：' + error.error.error + '\n\nFailed to generate memory: ' + error.error.error);
      } else {
        alert('更新记忆失败\n\nFailed to update memory');
      }
    }
  }

  // Load all memories for the user
  async loadAllMemories(): Promise<void> {
    this.isLoadingMemories = true;
    try {
      const response = await this.http.get<Memory[]>(`${this.API_URL}/api/memory/all`).toPromise();
      this.memories = response || [];

      // Select first bot if available
      if (this.memories.length > 0) {
        this.selectMemoryBot(this.memories[0].botId);
        this.cdr.markForCheck();
      }
    } catch (error) {
      console.error('Error loading memories:', error);
    } finally {
        this.cdr.markForCheck();
        this.isLoadingMemories = false;
    }
  }

  // Select a bot in memory management
  selectMemoryBot(botId: number): void {
    this.selectedMemoryBotId = botId;
    const memory = this.memories.find(m => m.botId === botId);
    this.memoryContent = memory ? memory.content : '';
    this.cdr.markForCheck();
  }

  // Save memory for selected bot
  async saveMemory(): Promise<void> {
    if (this.selectedMemoryBotId === null) return;

    try {
      await this.http.post(`${this.API_URL}/api/memory`, {
        botId: this.selectedMemoryBotId,
        content: this.memoryContent
      }).toPromise();

      // Update local memory
      const memoryIndex = this.memories.findIndex(m => m.botId === this.selectedMemoryBotId);
      if (memoryIndex >= 0) {
        this.memories[memoryIndex].content = this.memoryContent;
      }

      alert('记忆已保存\n\nMemory saved');
    } catch (error) {
      console.error('Error saving memory:', error);
      alert('保存记忆失败\n\nFailed to save memory');
    }
  }

  // Delete memory for selected bot
  async deleteMemory(): Promise<void> {
    if (this.selectedMemoryBotId === null) return;

    if (!confirm('确定要删除此记忆吗？\n\nAre you sure you want to delete this memory?')) {
      return;
    }

    try {
      await this.http.delete(`${this.API_URL}/api/memory?botId=${this.selectedMemoryBotId}`).toPromise();

      // Remove from local array
      this.memories = this.memories.filter(m => m.botId !== this.selectedMemoryBotId);

      // Select another bot or clear
      if (this.memories.length > 0) {
        this.selectMemoryBot(this.memories[0].botId);
      } else {
        this.selectedMemoryBotId = null;
        this.memoryContent = '';
      }

      alert('记忆已删除\n\nMemory deleted');
      this.cdr.markForCheck();
    } catch (error) {
      console.error('Error deleting memory:', error);
      alert('删除记忆失败\n\nFailed to delete memory');
      this.cdr.markForCheck();
    }
  }

  // Close memory dialog
  closeMemoryDialog(): void {
    this.showMemoryDialog = false;
  }

  // Show context menu on right click
  onMessageRightClick(event: MouseEvent, message: ChatMessage): void {
    event.preventDefault();
    this.contextMenuX = event.clientX;
    this.contextMenuY = event.clientY;
    this.contextMenuMessage = message;
    this.showContextMenu = true;
  }

  // Close context menu
  closeContextMenu(): void {
    this.showContextMenu = false;
    this.contextMenuMessage = null;
  }

  // Copy message content
  copyMessage(): void {
    if (!this.contextMenuMessage) return;

    navigator.clipboard.writeText(this.contextMenuMessage.content).then(() => {
      console.log('Message copied to clipboard');
    }).catch(err => {
      console.error('Failed to copy message:', err);
    });

    this.closeContextMenu();
  }

  // Delete a specific message
  async deleteMessage(): Promise<void> {
    if (!this.contextMenuMessage || !this.selectedBot) return;

    if (!confirm('确定要删除这条消息吗？\n\nAre you sure you want to delete this message?')) {
      this.closeContextMenu();
      return;
    }

    try {
      if (this.contextMenuMessage.datetime) {
        await this.http.delete(`${this.API_URL}/api/chat/message?botId=${this.selectedBot.id}&datetime=${encodeURIComponent(this.contextMenuMessage.datetime)}`).toPromise();

        // Remove from local array
        this.messages = this.messages.filter(m => m.id !== this.contextMenuMessage!.id);
        this.closeContextMenu();
      } else {
        // For messages without datetime (local only), just remove from array
        this.messages = this.messages.filter(m => m.id !== this.contextMenuMessage!.id);
        this.closeContextMenu();
      }
      this.cdr.markForCheck();
    } catch (error) {
      console.error('Error deleting message:', error);
      alert('删除消息失败\n\nFailed to delete message');
      this.closeContextMenu();
      this.cdr.markForCheck();
    }
  }
}
