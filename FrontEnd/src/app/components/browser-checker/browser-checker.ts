import {Component} from '@angular/core';
import {NgOptimizedImage} from '@angular/common';

@Component({
    selector: 'app-browser-checker',
    imports: [
        NgOptimizedImage
    ],
    templateUrl: './browser-checker.html',
    standalone: true,
    styleUrl: './browser-checker.scss'
})
export class BrowserChecker {

}
