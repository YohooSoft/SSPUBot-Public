import {Component} from '@angular/core';
import {NgForOf} from '@angular/common';
import {AboutAvatarComponent} from '../about-avatar-component/about-avatar-component';
import {TeamMember} from '../../interfaces/team-member';

@Component({
    selector: 'app-about-component',
    imports: [
        NgForOf,
        AboutAvatarComponent
    ],
    templateUrl: './about-component.html',
    styleUrl: './about-component.scss'
})
export class AboutComponent {
    teamMembers: TeamMember[] = [
        {
            avatar: 'https://www.helloimg.com/i/2025/07/12/6871f3952f300.jpg',
            name: 'Mryan2005',
            description: '项目开发者',
            link: 'https://mryan2005.top',
            type: 'url',
            githubUsername: 'Mryan2005'
        }
    ];

    openLink(link: string | null) {
        if (!link) {
            return;
        }
        window.open(link!, '_blank');
    }
}
