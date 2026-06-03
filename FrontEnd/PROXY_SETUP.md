# Angular Development Server Setup

## Starting the Development Server

To start the Angular development server with API proxy enabled:

```bash
npm start
```

Or explicitly:

```bash
ng serve
```

The proxy configuration in `src/proxy.conf.js` will automatically forward API requests:
- Requests to `http://localhost:4200/api/*` → `http://localhost:8080/*`

## Important Notes

1. **Restart Required**: If you modify `proxy.conf.js`, you must restart the dev server (`npm start`)
2. **Backend Must Be Running**: Ensure the Spring Boot backend is running on `http://localhost:8080`
3. **CORS**: The backend has CORS configured to allow requests from `http://localhost:4200`

## Troubleshooting

### 403 Forbidden on /api/auth/login

If you see `POST http://localhost:4200/api/auth/login 403 (Forbidden)`:

1. **Restart the dev server**: `Ctrl+C` then `npm start`
2. **Check backend is running**: Visit `http://localhost:8080/auth/status`
3. **Check proxy logs**: The console should show "Proxying request: POST /api/auth/login"
4. **Verify angular.json**: Should have `"proxyConfig": "src/proxy.conf.js"`

### Backend Connection Refused

If proxy shows connection errors:
- Ensure Spring Boot backend is running: `cd SSPUBotBackend && ./mvnw spring-boot:run`
- Check backend port: Should be `8080` (check `application.properties`)
