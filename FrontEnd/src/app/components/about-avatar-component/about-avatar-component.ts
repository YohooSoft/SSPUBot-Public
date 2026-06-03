import {Component, Input, OnInit} from '@angular/core';

@Component({
    selector: 'app-about-avatar-component',
    imports: [],
    templateUrl: './about-avatar-component.html',
    styleUrl: './about-avatar-component.scss'
})
export class AboutAvatarComponent implements OnInit {
    @Input() avatarUrl: string = '';
    @Input() githubUsername: string = '';
    @Input() type: AvatarSourceType = 'url'

    ngOnInit(): void {
        if (this.type === 'url' && !this.avatarUrl) {
            this.avatarUrl = 'https://www.helloimg.com/i/2025/07/12/6871f3952f300.jpg';
        } else if (this.type === 'github' && this.githubUsername) {
            this.avatarUrl = "https://github.com/" + this.githubUsername + ".png";
        }
    }
}
