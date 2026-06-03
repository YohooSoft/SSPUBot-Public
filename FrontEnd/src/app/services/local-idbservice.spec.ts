import { TestBed } from '@angular/core/testing';

import { LocalIDBService } from './local-idbservice';

describe('LocalIDBService', () => {
  let service: LocalIDBService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LocalIDBService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
