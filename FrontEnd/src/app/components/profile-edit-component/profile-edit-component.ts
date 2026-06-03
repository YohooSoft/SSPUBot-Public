import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-edit-component.html',
  styleUrls: ['./profile-edit-component.scss']
})
export class ProfileEditComponent implements OnInit {
  profile: any = {
    displayName: '',
    birthDate: null,
    isSchoolStudent: false,
    enrollmentDate: null,
    graduationDate: null,
    educationLevel: '',
    graduatedSchool: '',
    hobbies: '',
    fromLocation: '',
    wantToGo: ''
  };

  loading = false;
  saving = false;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.userService.getUserProfile().subscribe({
      next: (data) => {
        this.profile = {
          displayName: data.displayName || '',
          birthDate: data.birthDate ? this.formatDateForInput(data.birthDate) : null,
          isSchoolStudent: data.isSchoolStudent || false,
          enrollmentDate: data.enrollmentDate ? this.formatDateForInput(data.enrollmentDate) : null,
          graduationDate: data.graduationDate ? this.formatDateForInput(data.graduationDate) : null,
          educationLevel: data.educationLevel || '',
          graduatedSchool: data.graduatedSchool || '',
          hobbies: data.hobbies || '',
          fromLocation: data.fromLocation || '',
          wantToGo: data.wantToGo || ''
        };
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to load profile:', error);
        alert('加载用户资料失败');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  saveProfile(): void {
    this.saving = true;
    this.cdr.markForCheck();
    // Prepare data for backend
    const updates: any = {
      displayName: this.profile.displayName,
      isSchoolStudent: this.profile.isSchoolStudent,
      educationLevel: this.profile.educationLevel,
      graduatedSchool: this.profile.graduatedSchool,
      hobbies: this.profile.hobbies,
      fromLocation: this.profile.fromLocation,
      wantToGo: this.profile.wantToGo
    };

    // Convert dates to ISO format if provided
    if (this.profile.birthDate) {
      updates.birthDate = new Date(this.profile.birthDate).toISOString();
    }
    if (this.profile.enrollmentDate) {
      updates.enrollmentDate = new Date(this.profile.enrollmentDate).toISOString();
    }
    if (this.profile.graduationDate) {
      updates.graduationDate = new Date(this.profile.graduationDate).toISOString();
    }

    this.userService.updateUserProfile(updates).subscribe({
      next: () => {
        alert('用户资料已保存');
        this.saving = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Failed to save profile:', error);
        alert('保存用户资料失败');
        this.saving = false;
        this.cdr.markForCheck();
      }
    });
  }

  private formatDateForInput(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.toISOString().split('T')[0];
    } catch {
      return '';
    }
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
