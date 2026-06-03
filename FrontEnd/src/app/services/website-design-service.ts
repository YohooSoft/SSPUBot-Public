import {Injectable} from '@angular/core';

const DEFAULT_LANG = 'zh-CN';

@Injectable({
    providedIn: 'root'
})
export class WebsiteDesignService {
    websiteTitle = '二工大中央信息库';
    websiteFooterContent = '© 2025 Yan Zhizhong. All rights reserved.';
    currentLanguage = DEFAULT_LANG;
    uiMode = {
        normal: true,
        macOS: false
    }

    navLinksLeft = [
        {path: '/search', label: '查询'},
        {path: '/statistics', label: '统计'},
        {path: '/help', label: '帮助'},
        {path: '/about', label: '关于'},
    ];
    navLinksRight = [
        {path: '/chat', label: 'Ai'}
    ];

    footerDontFixedPages = [
        '^/help/.*',
        '/help',
        '/404',
        '/debug/char',
        '/chat',
        '^/admin.*',
        '/statistics',
        '/settings',
        '/bot-memory',
        '/profile',
        '/advancedSearch',
        ''
    ];

    // Easter Egg One
    loginForIEApi = '';
}
