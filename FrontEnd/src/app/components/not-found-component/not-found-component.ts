import {Component} from '@angular/core';
import {Location} from '@angular/common';

@Component({
    selector: 'app-not-found-component',
    imports: [],
    templateUrl: './not-found-component.html',
    standalone: true,
    styleUrl: './not-found-component.scss'
})
export class NotFoundComponent {
    constructor(private location: Location) {
    }

    goBack(): void {
        this.location.back();
    }
}
