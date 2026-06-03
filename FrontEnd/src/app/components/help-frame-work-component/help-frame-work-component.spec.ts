import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HelpFrameWorkComponent } from './help-frame-work-component';

describe('HelpFrameWorkComponent', () => {
  let component: HelpFrameWorkComponent;
  let fixture: ComponentFixture<HelpFrameWorkComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HelpFrameWorkComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HelpFrameWorkComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
