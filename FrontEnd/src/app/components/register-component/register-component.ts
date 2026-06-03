import {Component, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {WebsiteDesignService} from '../../services/website-design-service';
import {DeviceDetectorService} from 'ngx-device-detector';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-register-component',
  standalone: true,
  // Must import FormsModule to use ngModel, import CommonModule to use ngIf
  imports: [CommonModule, FormsModule],
  templateUrl: './register-component.html',
  styleUrl: './register-component.scss'
})
export class RegisterComponent implements OnInit {
  // Form model data
  username = '';
  email = '';
  password = '';
  confirmPassword = '';
  errorMessage = '';

  // Mock device info for *ngIf="deviceInfo?.browser !== 'Unknown'"
  // Replace with actual DeviceDetectorService logic
  deviceInfo: any = {
    browser: 'Chrome' // Default to modern browser, if set to 'Unknown' shows legacy form
  };

  // Use inject function instead of constructor injection
    private authService = inject(AuthService);
    private router = inject(Router);
    private deviceService = inject(DeviceDetectorService);
    private websiteDesignService = inject(WebsiteDesignService);

  ngOnInit(): void {
    // Perform actual device detection initialization here
    this.deviceInfo = this.deviceService.getDeviceInfo();
  }

  /**
   * Register logic
   */
  register() {
    this.errorMessage = '';

    // Basic validation
    if (this.password !== this.confirmPassword) {
      this.errorMessage = '两次输入的密码不一致';
      return;
    }

    // Call backend register API
    console.log('Submitting registration:', {
      username: this.username,
      email: this.email,
      password: this.password
    });

    this.authService.register({
        username: this.username,
        email: this.email,
        password: this.password
    }).subscribe({
        next: () => {
            alert('注册成功，请登录');
            this.navigateToLogin();
        },
        error: (err) => {
            console.error('Registration error:', err);
            this.errorMessage = err.error?.message || err.error || '注册失败，请稍后重试';
        }
    });
  }

  /**
   * Navigate back to login page
   */
  navigateToLogin() {
    this.router.navigate(['/login']);
  }
}
