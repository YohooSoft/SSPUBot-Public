import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService, Language } from '../../services/i18n.service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="language-switcher">
      <button 
        class="lang-button" 
        [class.active]="currentLang === 'zh'"
        (click)="switchLanguage('zh')"
        title="中文">
        🇨🇳 中文
      </button>
      <button 
        class="lang-button" 
        [class.active]="currentLang === 'en'"
        (click)="switchLanguage('en')"
        title="English">
        🇬🇧 EN
      </button>
    </div>
  `,
  styles: [`
    .language-switcher {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    .lang-button {
      padding: 0.5rem 1rem;
      background: rgba(255, 255, 255, 0.15);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 8px;
      cursor: pointer;
      font-size: 0.9rem;
      font-weight: 500;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 0.25rem;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

      &:hover {
        background: rgba(255, 255, 255, 0.25);
        border-color: rgba(255, 255, 255, 0.3);
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
      }

      &.active {
        background: rgba(76, 175, 80, 0.25);
        backdrop-filter: blur(20px) saturate(200%);
        -webkit-backdrop-filter: blur(20px) saturate(200%);
        border-color: rgba(76, 175, 80, 0.4);
        box-shadow: 0 4px 20px rgba(76, 175, 80, 0.3);
      }

      &:active {
        transform: translateY(0);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
    }

    @media (max-width: 768px) {
      .lang-button {
        padding: 0.4rem 0.8rem;
        font-size: 0.85rem;
      }
    }
  `]
})
export class LanguageSwitcherComponent {
  currentLang: Language;

  constructor(private i18nService: I18nService) {
    this.currentLang = this.i18nService.getCurrentLanguage();
    
    // Subscribe to language changes
    this.i18nService.currentLanguage$.subscribe(lang => {
      this.currentLang = lang;
    });
  }

  switchLanguage(lang: Language): void {
    this.i18nService.setLanguage(lang);
  }
}
