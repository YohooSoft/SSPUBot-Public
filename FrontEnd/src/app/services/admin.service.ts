import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LocalStorgeService } from './local-storge-service';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = 'http://localhost:8080/api/admin';

  constructor(
    private http: HttpClient,
    private localStorageService: LocalStorgeService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.localStorageService.getItemNormal('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // User Management
  getAllUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/users`, {
      headers: this.getHeaders()
    });
  }

  banUser(userId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/users/${userId}/ban`, {}, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }

  unbanUser(userId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/users/${userId}/unban`, {}, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }

  // Bot Management
  getAllBots(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/bots`, {
      headers: this.getHeaders()
    });
  }

  createBot(bot: any): Observable<any> {
    return this.http.post(`${this.API_URL}/bots`, bot, {
      headers: this.getHeaders()
    });
  }

  updateBot(id: number, bot: any): Observable<any> {
    return this.http.put(`${this.API_URL}/bots/${id}`, bot, {
      headers: this.getHeaders()
    });
  }

  deleteBot(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/bots/${id}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }

  // Spider Management
  getAllSpiders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/spiders`, {
      headers: this.getHeaders()
    });
  }

  createSpider(spider: any): Observable<any> {
    return this.http.post(`${this.API_URL}/spiders`, spider, {
      headers: this.getHeaders()
    });
  }

  updateSpider(id: number, spider: any): Observable<any> {
    return this.http.put(`${this.API_URL}/spiders/${id}`, spider, {
      headers: this.getHeaders()
    });
  }

  deleteSpider(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/spiders/${id}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }

  startSpider(id: number | string): Observable<any> {
    return this.http.post(`${this.API_URL}/spiders/${id}/start`, {}, {
      headers: this.getHeaders()
    });
  }

  stopSpider(id: number | string): Observable<any> {
    return this.http.post(`${this.API_URL}/spiders/${id}/stop`, {}, {
      headers: this.getHeaders()
    });
  }

  getSpiderProgress(id: number | string): Observable<any> {
    return this.http.get(`${this.API_URL}/spiders/${id}/progress`, {
      headers: this.getHeaders()
    });
  }

  // Synonym Management
  getAllSynonyms(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/synonyms`, {
      headers: this.getHeaders()
    });
  }

  getSynonymById(id: number): Observable<any> {
    return this.http.get(`${this.API_URL}/synonyms/${id}`, {
      headers: this.getHeaders()
    });
  }

  createSynonym(synonym: any): Observable<any> {
    return this.http.post(`${this.API_URL}/synonyms`, synonym, {
      headers: this.getHeaders()
    });
  }

  updateSynonym(id: number, synonym: any): Observable<any> {
    return this.http.put(`${this.API_URL}/synonyms/${id}`, synonym, {
      headers: this.getHeaders()
    });
  }

  deleteSynonym(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/synonyms/${id}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }
}
