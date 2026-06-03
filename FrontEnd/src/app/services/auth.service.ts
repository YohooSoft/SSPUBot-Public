import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { LocalStorgeService } from './local-storge-service';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  message?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private localStorageService = inject(LocalStorgeService);
  private router = inject(Router);

  // Use direct backend URL to bypass proxy issues
  private readonly API_URL = 'http://localhost:8080';

  /**
   * Login with username and password
   * Returns JWT token on success
   */
  login(loginData: LoginRequest): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    return this.http.post(`${this.API_URL}/auth/login`, loginData, { 
      headers,
      responseType: 'text'
    }).pipe(
      tap((token: string) => {
        console.log('Login successful, token received');
        // Store the JWT token as plain string
        this.localStorageService.setItemNormal('token', token);
      }),
      catchError((error) => {
        console.error('Login error:', error);
        throw error;
      })
    );
  }

  /**
   * Register a new user
   */
  register(registerData: RegisterRequest): Observable<string> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    return this.http.post(`${this.API_URL}/auth/register`, registerData, { 
      headers,
      responseType: 'text'
    });
  }

  /**
   * Logout the current user
   */
  logout(): void {
    this.localStorageService.removeItem('token');
    this.router.navigate(['/login']);
  }

  /**
   * Refresh the JWT token
   */
  refreshToken(): Observable<string> {
    const currentToken = this.getToken();
    if (!currentToken) {
      throw new Error('No token available to refresh');
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${currentToken}`
    });

    return this.http.post(`${this.API_URL}/auth/refresh`, {}, {
      headers,
      responseType: 'text'
    }).pipe(
      tap((newToken: string) => {
        // Update the stored token
        this.localStorageService.setItemNormal('token', newToken);
      })
    );
  }

  /**
   * Get the current JWT token
   */
  getToken(): string | null {
    return this.localStorageService.getItemNormal('token');
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /**
   * Parse JWT token to extract payload
   * Note: This does NOT validate the token - validation should be done on the backend
   */
  parseToken(token: string): any {
    try {
      // Validate JWT format (should have 3 parts separated by dots)
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.error('Invalid JWT format: token must have 3 parts');
        return null;
      }

      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (e) {
      console.error('Error parsing token:', e);
      return null;
    }
  }

  /**
   * Get current user information from token
   */
  getCurrentUser(): any {
    const token = this.getToken();
    if (!token) return null;
    return this.parseToken(token);
  }
}
