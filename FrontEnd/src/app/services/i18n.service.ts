import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type Language = 'zh' | 'en';

export interface Translations {
  [key: string]: string | Translations;
}

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private currentLanguageSubject = new BehaviorSubject<Language>('zh');
  public currentLanguage$: Observable<Language> = this.currentLanguageSubject.asObservable();

  private translations: { [key in Language]: Translations } = {
    zh: {
      home: {
        dailyQuiz: '每日一题',
        newCharacters: '今日新收录的字',
        newSentences: '今日新收录的句子',
        newWords: '今日新收录的词语',
        changeButton: '换一个',
        learnMore: '了解更多',
        recommendation: {
          location: '每日地点',
          tvDrama: '电视剧推荐',
          movie: '电影推荐',
          cantoneseOpera: '粤剧推荐',
          music: '音乐推荐'
        }
      },
      language: {
        switch: '切换语言',
        chinese: '中文',
        english: 'English'
      }
    },
    en: {
      home: {
        dailyQuiz: 'Daily Quiz',
        newCharacters: 'New Characters Today',
        newSentences: 'New Sentences Today',
        newWords: 'New Words Today',
        changeButton: 'Change',
        learnMore: 'Learn More',
        recommendation: {
          location: 'Daily Location',
          tvDrama: 'TV Drama',
          movie: 'Movie Recommendation',
          cantoneseOpera: 'Cantonese Opera',
          music: 'Music Recommendation'
        }
      },
      language: {
        switch: 'Switch Language',
        chinese: '中文',
        english: 'English'
      }
    }
  };

  constructor() {
    // Load saved language from localStorage (only in browser)
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      const savedLanguage = localStorage.getItem('language') as Language;
      if (savedLanguage && (savedLanguage === 'zh' || savedLanguage === 'en')) {
        this.currentLanguageSubject.next(savedLanguage);
      }
    }
  }

  getCurrentLanguage(): Language {
    return this.currentLanguageSubject.value;
  }

  setLanguage(language: Language): void {
    this.currentLanguageSubject.next(language);
    // Save to localStorage (only in browser)
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      localStorage.setItem('language', language);
    }
  }

  translate(key: string): string {
    const language = this.getCurrentLanguage();
    const keys = key.split('.');
    let result: any = this.translations[language];

    for (const k of keys) {
      if (result && typeof result === 'object') {
        result = result[k];
      } else {
        return key; // Return the key itself if translation not found
      }
    }

    return typeof result === 'string' ? result : key;
  }

  t(key: string): string {
    return this.translate(key);
  }
}
