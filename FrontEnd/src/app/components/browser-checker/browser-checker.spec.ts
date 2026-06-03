import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BrowserChecker } from './browser-checker';

describe('BrowserChecker', () => {
  let component: BrowserChecker;
  let fixture: ComponentFixture<BrowserChecker>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrowserChecker]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BrowserChecker);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
