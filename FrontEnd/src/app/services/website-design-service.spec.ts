import {TestBed} from '@angular/core/testing';

import {WebsiteDesignService} from './website-design-service';

describe('WebsiteContentService', () => {
    let service: WebsiteDesignService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(WebsiteDesignService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
