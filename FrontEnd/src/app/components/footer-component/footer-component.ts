import {Component} from '@angular/core';
import {WebsiteDesignService} from '../../services/website-design-service';

@Component({
    selector: 'app-footer-component',
    imports: [],
    templateUrl: './footer-component.html',
    styleUrl: './footer-component.scss'
})
export class FooterComponent {
    constructor(
        protected websiteDesignService: WebsiteDesignService
    ) {
    }
}
