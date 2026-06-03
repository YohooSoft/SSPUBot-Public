import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';

export interface CantoneseVoice {
  name: string;
  shortName: string;
  friendlyName: string;
  gender: string;
  locale: string;
}

export interface TTSRequest {
  text: string;
  voice?: string;
  rate?: string;
  volume?: string;
  pitch?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CantoneseTextToSpeechService {
  private readonly API_BASE = '/api/tts/cantonese';

  constructor(private http: HttpClient) {}

  /**
   * Convert text to speech in Cantonese
   * @param request TTS request with text and optional parameters
   * @returns Observable with audio blob
   */
  textToSpeech(request: TTSRequest): Observable<Blob> {
    return this.http.post(this.API_BASE, request, {
      responseType: 'blob'
    });
  }

  /**
   * Get available Cantonese voices
   * @returns Observable with list of available voices
   */
  getVoices(): Observable<{ voices: CantoneseVoice[] }> {
    return this.http.get<{ voices: CantoneseVoice[] }>(`${this.API_BASE}/voices`);
  }

  /**
   * Play text as speech
   * @param text Text to convert to speech
   * @param voice Optional voice name
   */
  async playText(text: string, voice?: string): Promise<void> {
    try {
      const blob = await firstValueFrom(this.textToSpeech({ text, voice }));
      if (blob) {
        const url = URL.createObjectURL(blob);
        const audio = new Audio(url);
        
        // Clean up the object URL after playback
        audio.addEventListener('ended', () => {
          URL.revokeObjectURL(url);
        });
        
        // Also clean up if there's an error
        audio.addEventListener('error', () => {
          URL.revokeObjectURL(url);
        });
        
        await audio.play();
      }
    } catch (error) {
      console.error('Error playing text:', error);
      throw error;
    }
  }
}
