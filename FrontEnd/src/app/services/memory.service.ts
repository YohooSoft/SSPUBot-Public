import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Memory {
  botId: number;
  botName?: string;
  content: string;
  createdAt?: string;
  updatedAt?: string;
}

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

@Injectable({
  providedIn: 'root'
})
export class MemoryService {
  private readonly API_URL = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  /**
   * Get all memories for the current user
   */
  getAllMemories(): Observable<Memory[]> {
    return this.http.get<Memory[]>(`${this.API_URL}/memory/all`);
  }

  /**
   * Get memory for a specific bot
   */
  getMemory(botId: number): Observable<Memory> {
    return this.http.get<Memory>(`${this.API_URL}/memory`, {
      params: { botId: botId.toString() }
    });
  }

  /**
   * Update memory for a specific bot
   */
  updateMemory(botId: number, content: string): Observable<any> {
    return this.http.post(`${this.API_URL}/memory`, { botId, content });
  }

  /**
   * Delete memory for a specific bot
   */
  deleteMemory(botId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/memory`, {
      params: { botId: botId.toString() }
    });
  }

  /**
   * Generate memory from chat history
   */
  generateMemory(botId: number): Observable<any> {
    return this.http.post(`${this.API_URL}/memory/generate`, { botId });
  }

  /**
   * Get all bots
   */
  getAllBots(): Observable<Bot[]> {
    return this.http.get<Bot[]>(`${this.API_URL}/bots`);
  }
}
