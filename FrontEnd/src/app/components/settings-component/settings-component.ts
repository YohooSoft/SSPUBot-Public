import {ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {SettingsInterface} from '../../interfaces/settings-interface';
import {isPlatformBrowser, CommonModule} from '@angular/common';
import {LocalStorgeService} from '../../services/local-storge-service';
import {RouterLink} from '@angular/router';
import {WebsiteDesignService} from '../../services/website-design-service';
import {LanguageSwitcherComponent} from '../language-switcher/language-switcher.component';
import {UserService} from '../../services/user.service';

@Component({
    selector: 'app-settings-component',
    imports: [
        FormsModule,
        RouterLink,
        LanguageSwitcherComponent,
        CommonModule
    ],
    templateUrl: './settings-component.html',
    standalone: true,
    styleUrl: './settings-component.scss'
})
export class SettingsComponent implements OnInit {
    settingContent: SettingsInterface = {
        isFilterBadWords: true,
        isFilterEnglishInHKCantonese: false
    }

    aiSettings: any = {
        allowBirthDate: false,
        allowIsSchoolStudent: false,
        allowEnrollmentDate: false,
        allowGraduationDate: false,
        allowEducationLevel: false,
        allowGraduatedSchool: false,
        allowHobbies: false,
        allowFromLocation: false,
        allowWantToGo: false
    };

    passwordForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
    };

    loadingSettings = false;
    changingPassword = false;
    passwordMessage = '';
    passwordMessageType: 'success' | 'error' = 'success';

    constructor(
        private localStorageService: LocalStorgeService,
        protected websiteDesignService: WebsiteDesignService,
        private userService: UserService,
        private cdr: ChangeDetectorRef,
        @Inject(PLATFORM_ID) private platformId: Object) {
    }

    ngOnInit(): void {
        if (isPlatformBrowser(this.platformId)) {
            const savedSettings = this.localStorageService.getItem('appSettings');
            if (savedSettings) {
                this.settingContent = savedSettings;
            } else {
                this.localStorageService.setItem('appSettings', this.settingContent);
            }

            // Load AI settings
            this.loadAiSettings();
        }
    }

    loadAiSettings(): void {
        this.loadingSettings = true;
        this.userService.getUserSettings().subscribe({
            next: (settings) => {
                this.aiSettings = {
                    allowBirthDate: settings.allowBirthDate || false,
                    allowIsSchoolStudent: settings.allowIsSchoolStudent || false,
                    allowEnrollmentDate: settings.allowEnrollmentDate || false,
                    allowGraduationDate: settings.allowGraduationDate || false,
                    allowEducationLevel: settings.allowEducationLevel || false,
                    allowGraduatedSchool: settings.allowGraduatedSchool || false,
                    allowHobbies: settings.allowHobbies || false,
                    allowFromLocation: settings.allowFromLocation || false,
                    allowWantToGo: settings.allowWantToGo || false
                };
                this.loadingSettings = false;
                this.cdr.markForCheck();
            },
            error: (error) => {
                console.error('Failed to load AI settings:', error);
                this.loadingSettings = false;
                this.cdr.markForCheck();
            }
        });
    }

    saveAiSettings(): void {
        this.userService.updateUserSettings(this.aiSettings).subscribe({
            next: () => {
                console.log('AI settings saved successfully');
            },
            error: (error) => {
                console.error('Failed to save AI settings:', error);
                alert('保存设置失败');
            }
        });
    }

    toggleAiSetting(setting: keyof typeof this.aiSettings): void {
        this.aiSettings[setting] = !this.aiSettings[setting];
        this.saveAiSettings();
    }

    changePassword(): void {
        // Clear previous message
        this.passwordMessage = '';

        // Validation
        if (!this.passwordForm.oldPassword) {
            this.passwordMessage = '请输入当前密码';
            this.passwordMessageType = 'error';
            return;
        }

        if (!this.passwordForm.newPassword) {
            this.passwordMessage = '请输入新密码';
            this.passwordMessageType = 'error';
            return;
        }

        if (this.passwordForm.newPassword.length < 6) {
            this.passwordMessage = '新密码长度至少为6位';
            this.passwordMessageType = 'error';
            return;
        }

        if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
            this.passwordMessage = '两次输入的新密码不一致';
            this.passwordMessageType = 'error';
            return;
        }

        this.changingPassword = true;
        this.userService.changePassword(this.passwordForm.oldPassword, this.passwordForm.newPassword).subscribe({
            next: (response) => {
                this.passwordMessage = '密码修改成功';
                this.passwordMessageType = 'success';
                this.changingPassword = false;
                // Clear form
                this.passwordForm = {
                    oldPassword: '',
                    newPassword: '',
                    confirmPassword: ''
                };
            },
            error: (error) => {
                console.error('Failed to change password:', error);
                this.passwordMessage = error.error?.error || '密码修改失败';
                this.passwordMessageType = 'error';
                this.changingPassword = false;
            }
        });
    }

    onSettingChange(item: any, needChangeValue: any): void {
        switch (item) {
            case 'FilterBadWords':
                if (needChangeValue) {
                    this.settingContent.isFilterBadWords = !this.settingContent.isFilterBadWords;
                }
                break;
            case 'FilterEnglishInHKCantonese':
                if (needChangeValue) {
                    this.settingContent.isFilterEnglishInHKCantonese = !this.settingContent.isFilterEnglishInHKCantonese;
                }
                break;
            case 'UiMode':
                if (needChangeValue === 'normal') {
                    this.websiteDesignService.uiMode.normal = true;
                    this.websiteDesignService.uiMode.macOS = false;
                } else if (needChangeValue === 'macOS') {
                    this.websiteDesignService.uiMode.normal = false;
                    this.websiteDesignService.uiMode.macOS = true;
                }
                break;
        }
        if (isPlatformBrowser(this.platformId)) {
            this.localStorageService.setItem('appSettings', this.settingContent);
        }
    }
}
