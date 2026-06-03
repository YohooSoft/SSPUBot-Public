import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LocalStorgeService } from '../services/local-storge-service';

/**
 * JWT HTTP Interceptor
 * Automatically adds JWT token to all HTTP requests if available
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const localStorageService = inject(LocalStorgeService);
  const token = localStorageService.getItemNormal('token');

  // List of auth endpoints that should not have Authorization header
  const authEndpoints = ['/api/auth/login', '/api/auth/register'];
  const isAuthEndpoint = authEndpoints.some(endpoint => req.url.endsWith(endpoint));

  // If token exists and request is not to auth endpoints, add Authorization header
  if (token && !isAuthEndpoint) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedRequest);
  }

  return next(req);
};
