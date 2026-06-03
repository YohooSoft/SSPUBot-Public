import {Component, Input} from '@angular/core';
import {MarkdownComponent} from 'ngx-markdown';

@Component({
    selector: 'app-markdown-component',
    imports: [
        MarkdownComponent
    ],
    templateUrl: './markdown-component.html',
    styleUrl: './markdown-component.scss'
})
export class MarkdownComponentNew {
    @Input() markdownContent: string = '';
}
