# Admin Page Documentation

## Overview

The Admin Page provides a centralized management interface for administrators to manage users, bots, and web spiders in the SSPUBot system.

## Access

- **URL**: `/admin`
- **Required Role**: `ADMIN` or `ROLE_ADMIN`
- **Authentication**: JWT token required

## Features

### 1. User Management

Administrators can view and manage all registered users in the system.

**Features:**
- View all users with their details (ID, username, display name, email, role, status, creation date)
- Ban users (except themselves)
- Unban users

**User Status:**
- `0` - Inactive
- `1` - Active (Normal)
- `2` - Banned
- `3` - Muted

**API Endpoints:**
- `GET /api/admin/users` - Get all users
- `PUT /api/admin/users/{userId}/ban` - Ban a user
- `PUT /api/admin/users/{userId}/unban` - Unban a user

### 2. Bot Management

Manage AI chatbots including their configuration and parameters.

**Features:**
- View all bots
- Create new bots
- Edit existing bots
- Delete bots
- Configure bot parameters:
  - Name
  - Description
  - Avatar URL
  - System Prompt
  - Selected Model
  - API Key
  - Base URL
  - Temperature (0.0 - 2.0)
  - Top-K (1 - 100)
  - Active status

**API Endpoints:**
- `GET /api/admin/bots` - Get all bots
- `POST /api/admin/bots` - Create a new bot
- `PUT /api/admin/bots/{id}` - Update a bot
- `DELETE /api/admin/bots/{id}` - Delete a bot

### 3. Spider Management

Manage web crawlers (spiders) for collecting data from various sources.

**Features:**
- View all spiders
- Create new spiders
- Edit spider configuration
- Delete spiders
- Start spider crawling
- View crawling progress
- Monitor spider status

**Spider Configuration:**
- Name
- Description
- Spider Class Name
- Start URLs (comma-separated)
- Allowed Domains (comma-separated)
- Active status

**Spider Status:**
- `idle` - Ready to run
- `running` - Currently crawling
- `stopped` - Manually stopped
- `error` - Encountered an error

**API Endpoints:**
- `GET /api/admin/spiders` - Get all spiders
- `POST /api/admin/spiders` - Create a new spider
- `PUT /api/admin/spiders/{id}` - Update a spider
- `DELETE /api/admin/spiders/{id}` - Delete a spider
- `POST /api/admin/spiders/{id}/start` - Start a spider
- `POST /api/admin/spiders/{id}/stop` - Stop a spider
- `GET /api/admin/spiders/{id}/progress` - Get spider progress

## Spider Integration

### Architecture

The spider management system consists of three layers:

1. **Admin Frontend (Angular)** - User interface for managing spiders
2. **Backend API (Spring Boot)** - Mediates between frontend and Python API
3. **Spider API (Flask)** - Manages Scrapy spider execution

### Starting the Spider API

Before using spider management features, start the Python Flask API:

```bash
cd SSPUBot/SpiderNew
pip install -r requirements.txt
python spider_api.py
```

The API runs on `http://localhost:5000`

### Available Spiders

The system includes the following pre-configured spiders:

1. **SpiderNewForJWC** - 教务处爬虫 (Academic Affairs Office)
2. **SpiderNewForSSPU** - 上海第二工业大学官网爬虫 (Main Website)  
3. **SpiderNewForSSPUPe2016** - 体育部爬虫 (Physical Education)
4. **SpiderNewForSSPUJXXY** - 继续教育学院爬虫 (Continuing Education)

### Managing Spiders Through Admin Page

Through the admin page, you can:

1. **View Spiders** - See all configured spiders with their status
2. **Create Spider** - Add new spider configurations (database only)
3. **Edit Spider** - Modify spider settings
4. **Delete Spider** - Remove spider configurations
5. **Start Spider** - Execute a spider (requires Spider API running)
6. **Stop Spider** - Halt a running spider
7. **View Progress** - Monitor spider execution status in real-time

**Important Notes:**
- Creating/editing spiders in the admin page only manages database configuration
- To create actual working spiders, add Python Scrapy spider files to `SSPUBot/SpiderNew/SpiderNew/spiders/`
- The spider execution requires the Flask Spider API to be running
- If the API is not available, status changes will be recorded but spiders won't actually execute

## Security

All admin endpoints are protected and require:
1. Valid JWT token in the Authorization header
2. User must have ADMIN or ROLE_ADMIN authority

Example Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Usage

### Accessing the Admin Page

1. Log in to the application with an admin account
2. Navigate to `/admin` in your browser
3. The page will automatically verify your admin privileges

### Managing Users

1. Click on the "用户管理" (User Management) tab
2. View the list of all users
3. To ban a user:
   - Click the "封禁" (Ban) button next to the user
   - Confirm the action
   - Note: You cannot ban yourself
4. To unban a user:
   - Click the "解封" (Unban) button next to a banned user
   - Confirm the action

### Managing Bots

1. Click on the "机器人管理" (Bot Management) tab
2. View the list of all bots
3. To add a new bot:
   - Click the "添加机器人" (Add Bot) button
   - Fill in the required fields (Name and System Prompt are required)
   - Optionally configure temperature (0.0-2.0) and Top-K (1-100)
   - Click "保存" (Save)
4. To edit a bot:
   - Click the "编辑" (Edit) button next to the bot
   - Modify the desired fields
   - Click "保存" (Save)
5. To delete a bot:
   - Click the "删除" (Delete) button
   - Confirm the action

### Managing Spiders

1. Click on the "爬虫管理" (Spider Management) tab
2. View the list of all spiders
3. To add a new spider:
   - Click the "添加爬虫" (Add Spider) button
   - Fill in the required fields (Name and Spider Class are required)
   - Configure start URLs and allowed domains
   - Click "保存" (Save)
4. To edit a spider:
   - Click the "编辑" (Edit) button
   - Modify the configuration
   - Click "保存" (Save)
5. To start a spider:
   - Click the "启动" (Start) button
   - The spider will begin crawling
6. To view progress:
   - Click the "查看进度" (View Progress) button
   - A dialog will show the current status and progress
7. To delete a spider:
   - Click the "删除" (Delete) button
   - Confirm the action

## Bot Configuration Parameters

### Temperature
- Range: 0.0 to 2.0
- Default: 0.7
- Lower values make responses more focused and deterministic
- Higher values make responses more creative and random

### Top-K
- Range: 1 to 100
- Default: 40
- Limits the number of tokens considered for sampling
- Lower values make responses more focused
- Higher values increase diversity

## Database Schema

### Spider Table
```sql
CREATE TABLE spiders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    spider_class VARCHAR(255) NOT NULL,
    start_urls TEXT,
    allowed_domains TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    status VARCHAR(50) DEFAULT 'idle',
    progress INT DEFAULT 0,
    last_error TEXT,
    last_run_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Bot Table (Updated)
```sql
ALTER TABLE bots ADD COLUMN temperature DOUBLE;
ALTER TABLE bots ADD COLUMN top_k INT;
```

## Error Handling

All admin operations include error handling:
- Unauthorized access attempts return HTTP 403
- Invalid operations return HTTP 400 with error messages
- Not found resources return HTTP 404
- Server errors return HTTP 500 with error details

## Future Enhancements

The following features are planned for future releases:
1. Integration with Python spider execution engine
2. Real-time spider progress updates via WebSocket
3. Spider scheduling and cron-based execution
4. User activity logs and audit trail
5. Bulk user operations
6. Bot performance analytics
7. Spider data visualization

## Troubleshooting

### Cannot Access Admin Page
- Ensure you are logged in
- Verify your user has ADMIN role in the database
- Check browser console for errors
- Verify JWT token is valid

### Cannot Start Spider
- Check spider configuration is valid
- Ensure spider class exists
- Verify start URLs are accessible
- Check server logs for detailed errors

### Bot Creation Fails
- Ensure name is unique
- Verify system prompt is not empty
- Check all required fields are filled
- Validate temperature and top-k values are in range
