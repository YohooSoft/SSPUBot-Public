import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LocalStorgeService } from './local-storge-service';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly API_URL = 'http://localhost:8080/api/user';

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

  // User Profile Management
  getUserProfile(): Observable<any> {
    return this.http.get(`${this.API_URL}/profile`, {
      headers: this.getHeaders()
    });
  }

  updateUserProfile(updates: any): Observable<any> {
    return this.http.put(`${this.API_URL}/profile`, updates, {
      headers: this.getHeaders()
    });
  }

  // User Settings Management
  getUserSettings(): Observable<any> {
    return this.http.get(`${this.API_URL}/settings`, {
      headers: this.getHeaders()
    });
  }

  updateUserSettings(settings: any): Observable<any> {
    return this.http.put(`${this.API_URL}/settings`, settings, {
      headers: this.getHeaders()
    });
  }

  // Password Management
  changePassword(oldPassword: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.API_URL}/change-password`, {
      oldPassword,
      newPassword
    }, {
      headers: this.getHeaders()
    });
  }
}
