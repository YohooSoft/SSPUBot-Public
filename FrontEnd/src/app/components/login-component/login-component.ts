import {Component, inject} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgIf} from '@angular/common';
import {DeviceDetectorService, DeviceInfo} from 'ngx-device-detector';
import {WebsiteDesignService} from '../../services/website-design-service';
import {AuthService, LoginRequest} from '../../services/auth.service';
import {Router} from '@angular/router';

@Component({
    selector: 'app-login-component',
    imports: [
        FormsModule,
        NgIf
    ],
    templateUrl: './login-component.html',
    standalone: true,
    styleUrl: './login-component.scss'
})
export class LoginComponent {
    username: string = '';
    password: string = '';
    errorMessage: string = '';
    deviceInfo: DeviceInfo | null = null;
    isLoading: boolean = false;

    // Use inject function instead of constructor injection
    private deviceService = inject(DeviceDetectorService);
    private authService = inject(AuthService);
    private router = inject(Router);
    protected websiteDesignService = inject(WebsiteDesignService);

    ngOnInit(): void {
        this.deviceInfo = this.deviceService.getDeviceInfo();
    }

    login() {
        if (!this.username || !this.password) {
            this.errorMessage = '请输入用户名和密码';
            return;
        }

        this.isLoading = true;
        this.errorMessage = '';

        const loginData: LoginRequest = {
            username: this.username,
            password: this.password
        };

        console.log('Attempting login with:', { username: this.username });

        this.authService.login(loginData).subscribe({
            next: (token) => {
                console.log('Login successful, token received');
                this.isLoading = false;
                alert('登录成功！');
                // Navigate to home page
                this.router.navigate(['/']);
            },
            error: (err) => {
                console.error('Login error:', err);
                this.isLoading = false;
                // Handle different error scenarios
                if (err.status === 0) {
                    this.errorMessage = '无法连接到服务器，请确保后端服务正在运行';
                } else if (err.status === 401 || err.status === 403) {
                    this.errorMessage = '用户名或密码错误';
                } else if (err.error) {
                    this.errorMessage = typeof err.error === 'string' ? err.error : '登录失败，请稍后重试';
                } else {
                    this.errorMessage = '登录失败: ' + (err.message || '未知错误');
                }
            }
        });
    }

    navigateToRegister() {
        this.router.navigate(['/register']);
    }

    navigateToForgotPassword() {
        this.router.navigate(['/forgot-password']);
    }
}
