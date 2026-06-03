import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AboutAvatarComponent } from './about-avatar-component';

describe('AboutAvatarComponent', () => {
  let component: AboutAvatarComponent;
  let fixture: ComponentFixture<AboutAvatarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AboutAvatarComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AboutAvatarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
