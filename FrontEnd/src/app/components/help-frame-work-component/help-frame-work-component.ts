import {Component, HostListener, OnInit} from '@angular/core';
import {ActivatedRoute, Router, RouterLink, RouterOutlet} from '@angular/router';
import {NgForOf} from '@angular/common';

@Component({
    selector: 'app-help-frame-work-component',
    imports: [
        RouterOutlet,
        RouterLink,
        NgForOf
    ],
    templateUrl: './help-frame-work-component.html',
    styleUrl: './help-frame-work-component.scss'
})
export class HelpFrameWorkComponent implements OnInit {

    sidebarWidth = 250; // 初始宽度
    private resizing = false;

    menuItems = [
        {name: '查询模块', route: '/help/query'},
        {name: '统计模块', route: '/help/statistics'},
        {name: 'AI模块', route: '/help/ai'},
        {name: '管理员', route: '/help/admin'},
        {name: '个人资料', route: '/help/profile'},
        {name: '设置', route: '/help/settings'},
    ];
    private whichPage: string | null = '';

    constructor(
        private route: ActivatedRoute,
        private router: Router,
    ) {
    }

    onResizeStart(event: MouseEvent): void {
        this.resizing = true;
        event.preventDefault();
    }

    @HostListener('document:mousemove', ['$event'])
    onResizing(event: MouseEvent): void {
        if (this.resizing) {
            const newWidth = event.clientX;
            if (newWidth > 100 && newWidth < 500) { // 限制宽度范围
                this.sidebarWidth = newWidth;
            }
        }
    }

    @HostListener('document:mouseup')
    onResizeEnd(): void {
        this.resizing = false;
    }

    onButtonClick(item: string) {

    }

    ngOnInit(): void {
    }

    isActive(route: string) {
        return window.location.pathname === route;
    }
}
