# Spider Management API

This Flask-based API service provides REST endpoints to manage and execute Scrapy spiders.

## Installation

1. Install Python dependencies:
```bash
cd SSPUBot/SpiderNew
pip install -r requirements.txt
```

## Running the API

Start the Flask server:
```bash
cd SSPUBot/SpiderNew
python spider_api.py
```

The API will be available at `http://localhost:5000`

## API Endpoints

### List Available Spiders
```
GET /api/spiders
```

Returns a list of all available spiders with their current status.

### Start a Spider
```
POST /api/spiders/<spider_id>/start
```

Starts the specified spider. Returns an error if the spider is already running.

### Stop a Spider
```
POST /api/spiders/<spider_id>/stop
```

Stops a running spider.

### Get Spider Status
```
GET /api/spiders/<spider_id>/status
```

Returns the current status and progress of a spider.

### Health Check
```
GET /health
```

Returns the health status of the API service.

## Available Spiders

The following spiders are available:

1. **SpiderNewForJWC** - 教务处爬虫
   - Crawls data from the Academic Affairs Office website

2. **SpiderNewForSSPU** - 上海第二工业大学官网爬虫
   - Crawls data from the main SSPU website

3. **SpiderNewForSSPUPe2016** - 体育部爬虫
   - Crawls data from the Physical Education Department

4. **SpiderNewForSSPUJXXY** - 继续教育学院爬虫
   - Crawls data from the Continuing Education College

## Integration with Backend

The Java Spring Boot backend communicates with this Python API to:
- Start and stop spiders
- Monitor spider progress
- Retrieve spider status

The backend's `SpiderExecutionService` handles this communication.

## Architecture

```
Admin Page (Angular) 
    ↓ HTTP
Backend (Spring Boot)
    ↓ HTTP (port 5000)
Python Spider API (Flask)
    ↓ Process execution
Scrapy Spiders
```

## Running Spiders Manually

You can also run spiders directly using the traditional method:

```bash
cd SSPUBot/SpiderNew
python run_spiders.py
```

Or run individual spiders:

```bash
scrapy crawl SpiderNewForJWC
```

## Troubleshooting

### API Not Starting
- Check if port 5000 is available
- Verify Python dependencies are installed
- Check Python version (requires Python 3.8+)

### Spider Fails to Start
- Check spider configuration in the database
- Verify spider class name matches actual spider class
- Check Scrapy settings in `SpiderNew/settings.py`

### Connection Refused
- Ensure the Flask API is running
- Check firewall settings
- Verify the backend is configured to use `http://localhost:5000`
