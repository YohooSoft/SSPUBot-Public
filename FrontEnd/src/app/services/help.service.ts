import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

interface HelpContent {
  title: string;
  description: string;
  features: string[];
  usage: string[];
}

@Injectable({
  providedIn: 'root'
})
export class HelpService {
  private apiUrl = `${environment.apiUrl}/api/help`;

  constructor(private http: HttpClient) {}

  getModuleHelp(module: string): Observable<HelpContent> {
    return this.http.get<HelpContent>(`${this.apiUrl}/${module}`).pipe(
      catchError(error => {
        console.error(`Error fetching help content for ${module}:`, error);
        throw error;
      })
    );
  }
}
