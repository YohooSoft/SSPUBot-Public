import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterLink, RouterOutlet} from '@angular/router';
import {TopbarComponent} from './components/topbar-component/topbar-component';
import {WebsiteDesignService} from './services/website-design-service';
import {Title} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {FooterComponent} from './components/footer-component/footer-component';
import {WasmService} from './services/wasm-service';
import {DeviceDetectorService, DeviceInfo} from 'ngx-device-detector';
import {UserProfileComponent} from './components/user-profile-component/user-profile-component';
import {AuthService} from './services/auth.service';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, TopbarComponent, RouterLink, CommonModule, FooterComponent, UserProfileComponent],
    templateUrl: './app.html',
    styleUrl: './app.scss'
})
export class App implements OnInit {
    whichPage: string | null = '';
    regex: RegExp | null = null;
    matched: boolean = false;
    deviceInfo: DeviceInfo | null = null;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        protected websiteDesignService: WebsiteDesignService,
        protected wasmService: WasmService,
        private titleService: Title,
        private deviceService: DeviceDetectorService,
        private authService: AuthService
    ) {
    }

    isLoggedIn(): boolean {
        return this.authService.isLoggedIn();
    }

    isAdmin(): boolean {
        const user = this.authService.getCurrentUser();
        return user && (user.role === 'ADMIN' || user.role === 'ROLE_ADMIN');
    }

    ngOnInit(): void {
        this.regex = new RegExp(this.websiteDesignService.footerDontFixedPages.join('|'));
        this.titleService.setTitle(this.websiteDesignService.websiteTitle);
        this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                this.whichPage = this.router.url;
                console.debug(`当前 URL: ${this.whichPage}`);
                this.matched = this.regex!.test(this.whichPage!);
                console.debug(`匹配结果: ${this.matched}`);
            }
        });
        this.deviceInfo = this.deviceService.getDeviceInfo();
        this.websiteDesignService.currentLanguage = navigator.language;
        console.debug('设备信息:', this.deviceInfo, 'navigator.language:', navigator.language);
    }
}
