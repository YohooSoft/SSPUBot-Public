import { Component, inject, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-profile-component.html',
  styleUrls: ['./user-profile-component.scss']
})
export class UserProfileComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  displayName: string = '';
  isDropdownOpen: boolean = false;
  isAdmin: boolean = false;

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user && user.displayName) {
      this.displayName = user.displayName;
    } else if (user && user.sub) {
      // Fallback to username if displayName not available
      this.displayName = user.sub;
    }
    
    // Check if user is admin
    this.isAdmin = user && (user.role === 'ADMIN' || user.role === 'ROLE_ADMIN');
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const dropdown = document.querySelector('.user-profile-dropdown');
    
    if (dropdown && !dropdown.contains(target)) {
      this.isDropdownOpen = false;
    }
  }

  navigateToProfile(): void {
    this.isDropdownOpen = false;
    this.router.navigate(['/profile']);
  }

  navigateToBotMemory(): void {
    this.isDropdownOpen = false;
    this.router.navigate(['/bot-memory']);
  }

  navigateToSettings(): void {
    this.isDropdownOpen = false;
    this.router.navigate(['/settings']);
  }

  navigateToAdmin(): void {
    this.isDropdownOpen = false;
    this.router.navigate(['/admin']);
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.authService.logout();
  }
}
