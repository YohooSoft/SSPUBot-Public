"""
Spider Management API Service
This Flask service provides REST API endpoints to manage and execute Scrapy spiders.
"""

from flask import Flask, jsonify, request
from flask_cors import CORS
# ---------------------------------------------------------------------------
# CRITICAL FIX: DELETED TOP-LEVEL SCRAPY/TWISTED IMPORTS
# They are now moved inside the worker function (run_spider_in_process)
# ---------------------------------------------------------------------------
from multiprocessing import Process, Queue
import os
import sys
import json
import logging
import pymssql
from datetime import datetime
import importlib
import inspect
import asyncio

# Add the SpiderNew directory to the path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

app = Flask(__name__)
CORS(app)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Database configuration
DB_CONFIG = {
    'server': 'localhost',
    'port': 1433,
    'database': 'database',
    'user': 'user',
    'password': 'password'
}

# Storage for spider execution status
spider_status = {}

# Storage for spider configurations (persisted to JSON file)
SPIDER_CONFIG_FILE = 'spider_config.json'


def get_db_connection():
    """Create and return a database connection"""
    try:
        conn = pymssql.connect(
            server=DB_CONFIG['server'],
            user=DB_CONFIG['user'],
            password=DB_CONFIG['password'],
            database=DB_CONFIG['database'],
            port=DB_CONFIG['port']
        )
        return conn
    except Exception as e:
        logger.error(f"Database connection error: {e}")
        return None


def init_spider_table():
    """Initialize spider_status table in database"""
    conn = get_db_connection()
    if not conn:
        return False

    try:
        cursor = conn.cursor()
        # Create table if not exists
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='spider_status' AND xtype='U')
            CREATE TABLE spider_status (
                spider_id VARCHAR(255) PRIMARY KEY,
                spider_name VARCHAR(255) NOT NULL,
                status VARCHAR(50) NOT NULL,
                started_at DATETIME NULL,
                last_run_time DATETIME NULL,
                completed_at DATETIME NULL,
                runtime_seconds INT NULL,
                error_message VARCHAR(MAX) NULL,
                created_at DATETIME DEFAULT GETDATE(),
                updated_at DATETIME DEFAULT GETDATE()
            )
        """)
        conn.commit()
        
        # Clear all existing records when spider_api.py restarts
        cursor.execute("DELETE FROM spider_status")
        conn.commit()
        logger.info("Cleared all spider_status records on startup")
        
        cursor.close()
        conn.close()
        return True
    except Exception as e:
        logger.error(f"Error initializing spider table: {e}")
        if conn:
            conn.close()
        return False


def save_spider_status_to_db(spider_id, spider_name, status, started_at=None, last_run_time=None,
                              completed_at=None, runtime_seconds=None, error_message=None):
    """Save spider status to database"""
    conn = get_db_connection()
    if not conn:
        return False

    try:
        cursor = conn.cursor()
        # Check if record exists
        cursor.execute("SELECT COUNT(*) FROM spider_status WHERE spider_id = %s", (spider_id,))
        exists = cursor.fetchone()[0] > 0

        if exists:
            # Update existing record
            query = """
                UPDATE spider_status 
                SET spider_name = %s, status = %s, started_at = %s, last_run_time = %s,
                    completed_at = %s, runtime_seconds = %s, error_message = %s, updated_at = GETDATE()
                WHERE spider_id = %s
            """
            cursor.execute(query, (spider_name, status, started_at, last_run_time,
                                 completed_at, runtime_seconds, error_message, spider_id))
        else:
            # Insert new record
            query = """
                INSERT INTO spider_status (spider_id, spider_name, status, started_at, last_run_time,
                                         completed_at, runtime_seconds, error_message)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """
            cursor.execute(query, (spider_id, spider_name, status, started_at, last_run_time,
                                 completed_at, runtime_seconds, error_message))

        conn.commit()
        cursor.close()
        conn.close()
        return True
    except Exception as e:
        logger.error(f"Error saving spider status to DB: {e}")
        if conn:
            conn.close()
        return False


def get_spider_status_from_db(spider_id):
    """Get spider status from database"""
    conn = get_db_connection()
    if not conn:
        return None

    try:
        cursor = conn.cursor(as_dict=True)
        cursor.execute("SELECT * FROM spider_status WHERE spider_id = %s", (spider_id,))
        result = cursor.fetchone()
        cursor.close()
        conn.close()
        return result
    except Exception as e:
        logger.error(f"Error getting spider status from DB: {e}")
        if conn:
            conn.close()
        return None


def discover_spiders():
    """Automatically discover spiders from the spiders directory"""
    spiders = {}
    spiders_dir = os.path.join(os.path.dirname(__file__), 'SpiderNew', 'spiders')

    try:
        if os.path.exists(spiders_dir):
            # List all Python files in spiders directory
            for filename in os.listdir(spiders_dir):
                if filename.endswith('.py') and not filename.startswith('__'):
                    module_name = filename[:-3]
                    try:
                        # Import the module
                        module = importlib.import_module(f'SpiderNew.spiders.{module_name}')

                        # Find spider classes
                        for name, obj in inspect.getmembers(module):
                            # Check if it looks like a spider class (has name attribute)
                            # We avoid importing scrapy.Spider here to prevent reactor init
                            if inspect.isclass(obj) and hasattr(obj, 'name') and name != 'Spider':
                                spider_name = obj.name if isinstance(obj.name, str) else module_name
                                spider_id = spider_name

                                # Get spider attributes
                                allowed_domains = getattr(obj, 'allowed_domains', [])
                                start_urls = getattr(obj, 'start_urls', [])

                                spiders[spider_id] = {
                                    'id': spider_id,
                                    'name': spider_name,
                                    'class': name,
                                    'description': f'{spider_name} 爬虫',
                                    'module': f'SpiderNew.spiders.{module_name}',
                                    'spiderClass': name,
                                    'startUrls': start_urls[0] if start_urls else '',
                                    'allowedDomains': allowed_domains[0] if allowed_domains else '',
                                    'isActive': True,
                                    'createdAt': datetime.now().isoformat(),
                                    'updatedAt': datetime.now().isoformat()
                                }
                                logger.info(f"Discovered spider: {spider_id}")
                                break  # Only take first spider class from each file

                    except Exception as e:
                        logger.error(f"Error importing spider from {filename}: {e}")
    except Exception as e:
        logger.error(f"Error discovering spiders: {e}")

    # If no spiders discovered, use defaults (Fallback)
    if not spiders:
        spiders = {
            'SpiderNewForJWC': {
                'id': 'SpiderNewForJWC',
                'name': 'SpiderNewForJWC',
                'class': 'SpidernewforjwcSpider',
                'description': '教务处爬虫',
                'module': 'SpiderNew.spiders.SpiderNewForJWC',
                'spiderClass': 'SpidernewforjwcSpider',
                'startUrls': 'http://jwc.sspu.edu.cn/',
                'allowedDomains': 'jwc.sspu.edu.cn',
                'isActive': True,
                'createdAt': '2024-01-01T00:00:00',
                'updatedAt': '2024-01-01T00:00:00'
            },
            # ... (Other defaults omitted for brevity, logic remains)
        }

    return spiders


# Initialize database table
init_spider_table()

# Discover available spiders
AVAILABLE_SPIDERS = discover_spiders()


def load_spider_config():
    """Load spider configuration from JSON file"""
    if os.path.exists(SPIDER_CONFIG_FILE):
        try:
            with open(SPIDER_CONFIG_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Error loading spider config: {e}")
    return AVAILABLE_SPIDERS.copy()


def save_spider_config(config):
    """Save spider configuration to JSON file"""
    try:
        with open(SPIDER_CONFIG_FILE, 'w', encoding='utf-8') as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
        return True
    except Exception as e:
        logger.error(f"Error saving spider config: {e}")
        return False


# Load spider configuration on startup
spider_config = load_spider_config()


# ---------------------------------------------------------------------------
# CRITICAL FIX: Worker function with correct Reactor Setup
# ---------------------------------------------------------------------------
def run_spider_in_process(spider_name, result_queue, shutdown_queue):
    """Run a spider in a separate process with graceful shutdown support"""

    # 1. WINDOWS FIX: Set Event Loop Policy
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    # 2. INSTALL REACTOR: Must happen before any Scrapy/Twisted import
    from twisted.internet import asyncioreactor
    if 'twisted.internet.reactor' not in sys.modules:
        asyncioreactor.install()

    # 3. NOW IMPORT SCRAPY
    from scrapy.crawler import CrawlerProcess
    from scrapy.utils.project import get_project_settings
    from twisted.internet import reactor, task

    # Graceful shutdown checker
    def check_shutdown():
        """Check if shutdown signal has been sent"""
        try:
            if not shutdown_queue.empty():
                signal = shutdown_queue.get_nowait()
                if signal == 'stop':
                    logger.info(f"Graceful shutdown signal received for {spider_name}")
                    # Stop the crawler gracefully
                    if hasattr(crawler, 'engine') and crawler.engine:
                        crawler.engine.close_spider(crawler.spider, 'shutdown')
                    else:
                        reactor.stop()
        except:
            pass

    try:
        settings = get_project_settings()
        process = CrawlerProcess(settings)
        crawler = process.create_crawler(spider_name)
        process.crawl(crawler)
        
        # Set up periodic check for shutdown signal (every 1 second)
        shutdown_checker = task.LoopingCall(check_shutdown)
        shutdown_checker.start(1.0)
        
        # Start the reactor
        process.start()
        result_queue.put({'status': 'completed', 'spider': spider_name})
    except Exception as e:
        logger.error(f"Error in spider process {spider_name}: {e}")
        result_queue.put({'status': 'error', 'spider': spider_name, 'error': str(e)})


@app.route('/api/spiders', methods=['GET'])
def list_spiders():
    """List all available spiders"""
    try:
        spiders = []
        for spider_id, spider_info in spider_config.items():
            # Get status from database
            db_status = get_spider_status_from_db(spider_id)

            if db_status:
                status = db_status['status']
                last_run_time = db_status['last_run_time'].isoformat() if db_status['last_run_time'] else None
                started_at = db_status['started_at'].isoformat() if db_status['started_at'] else None
                runtime_seconds = db_status['runtime_seconds'] or 0
                last_error = db_status['error_message']
            else:
                # Fallback to memory status
                mem_status = spider_status.get(spider_id, {})
                status = mem_status.get('status', 'idle')
                last_run_time = mem_status.get('last_run')
                started_at = mem_status.get('started_at')
                runtime_seconds = 0
                last_error = mem_status.get('error')

            spiders.append({
                'id': spider_info.get('id', spider_id),
                'name': spider_info['name'],
                'description': spider_info.get('description', ''),
                'spiderClass': spider_info.get('spiderClass', spider_info.get('class', '')),
                'startUrls': spider_info.get('startUrls', ''),
                'allowedDomains': spider_info.get('allowedDomains', ''),
                'isActive': spider_info.get('isActive', True),
                'status': status,
                'startedAt': started_at,  # Added for client-side timer
                'runtimeSeconds': runtime_seconds,
                'lastRunTime': last_run_time,
                'lastError': last_error,
                'createdAt': spider_info.get('createdAt', ''),
                'updatedAt': spider_info.get('updatedAt', '')
            })
        return jsonify(spiders), 200
    except Exception as e:
        logger.error(f"Error listing spiders: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/spiders', methods=['POST'])
def create_spider():
    """Create a new spider configuration"""
    try:
        data = request.get_json()

        if not data or 'name' not in data:
            return jsonify({'error': 'Spider name is required'}), 400

        spider_id = data['name']

        if spider_id in spider_config:
            return jsonify({'error': 'Spider with this name already exists'}), 400

        # Create new spider configuration
        spider_config[spider_id] = {
            'id': spider_id,
            'name': data['name'],
            'description': data.get('description', ''),
            'spiderClass': data.get('spiderClass', data['name']),
            'startUrls': data.get('startUrls', ''),
            'allowedDomains': data.get('allowedDomains', ''),
            'isActive': data.get('isActive', True),
            'module': f'SpiderNew.spiders.{data["name"]}',
            'createdAt': datetime.now().isoformat(),
            'updatedAt': datetime.now().isoformat()
        }

        # Save to file
        if save_spider_config(spider_config):
            logger.info(f"Created spider: {spider_id}")
            return jsonify(spider_config[spider_id]), 201
        else:
            return jsonify({'error': 'Failed to save spider configuration'}), 500

    except Exception as e:
        logger.error(f"Error creating spider: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/spiders/<spider_id>', methods=['PUT'])
def update_spider(spider_id):
    """Update a spider configuration"""
    try:
        if spider_id not in spider_config:
            return jsonify({'error': 'Spider not found'}), 404

        data = request.get_json()
        if not data:
            return jsonify({'error': 'Request body is required'}), 400

        # Update spider configuration
        spider_config[spider_id].update({
            'name': data.get('name', spider_config[spider_id]['name']),
            'description': data.get('description', spider_config[spider_id].get('description', '')),
            'spiderClass': data.get('spiderClass', spider_config[spider_id].get('spiderClass', '')),
            'startUrls': data.get('startUrls', spider_config[spider_id].get('startUrls', '')),
            'allowedDomains': data.get('allowedDomains', spider_config[spider_id].get('allowedDomains', '')),
            'isActive': data.get('isActive', spider_config[spider_id].get('isActive', True)),
            'updatedAt': datetime.now().isoformat()
        })

        # Save to file
        if save_spider_config(spider_config):
            logger.info(f"Updated spider: {spider_id}")
            return jsonify(spider_config[spider_id]), 200
        else:
            return jsonify({'error': 'Failed to save spider configuration'}), 500

    except Exception as e:
        logger.error(f"Error updating spider: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/spiders/<spider_id>', methods=['DELETE'])
def delete_spider(spider_id):
    """Delete a spider configuration"""
    try:
        if spider_id not in spider_config:
            return jsonify({'error': 'Spider not found'}), 404

        # Don't allow deletion of running spiders
        status = spider_status.get(spider_id, {})
        if status.get('status') == 'running':
            return jsonify({'error': 'Cannot delete a running spider'}), 400

        # Remove from configuration
        del spider_config[spider_id]

        # Save to file
        if save_spider_config(spider_config):
            logger.info(f"Deleted spider: {spider_id}")
            return jsonify({'message': f'Spider {spider_id} deleted successfully'}), 200
        else:
            return jsonify({'error': 'Failed to save spider configuration'}), 500

    except Exception as e:
        logger.error(f"Error deleting spider: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/spiders/<spider_id>/start', methods=['POST'])
def start_spider(spider_id):
    """Start a spider"""
    try:
        if spider_id not in spider_config:
            return jsonify({'error': 'Spider not found'}), 404

        # Check if already running
        current_status = spider_status.get(spider_id, {})
        if current_status.get('status') == 'running':
            # Double check if process is actually dead
            proc = current_status.get('process')
            if proc and not proc.is_alive():
                pass # It's actually dead, let it restart
            else:
                return jsonify({'error': 'Spider is already running'}), 400

        # Get current timestamp
        now = datetime.now()

        # Create shutdown queue for graceful shutdown
        shutdown_queue = Queue()
        
        # Update status in memory
        spider_status[spider_id] = {
            'status': 'running',
            'started_at': now.isoformat(),
            'last_run': now.isoformat(),
            'shutdown_queue': shutdown_queue
        }

        # Save status to database
        spider_name = spider_config[spider_id]['name']
        save_spider_status_to_db(
            spider_id=spider_id,
            spider_name=spider_name,
            status='running',
            started_at=now,
            last_run_time=now
        )

        # Start spider in a separate process
        result_queue = Queue()
        spider_process = Process(
            target=run_spider_in_process,
            args=(spider_id, result_queue, shutdown_queue)
        )
        spider_process.start()

        # Store process info
        spider_status[spider_id]['process'] = spider_process

        logger.info(f"Started spider: {spider_id}")
        return jsonify({
            'message': f'Spider {spider_id} started',
            'status': 'running'
        }), 200

    except Exception as e:
        logger.error(f"Error starting spider {spider_id}: {str(e)}")
        spider_status[spider_id] = {
            'status': 'error',
            'error': str(e),
            'last_run': datetime.now().isoformat()
        }
        # Save error to database
        save_spider_status_to_db(
            spider_id=spider_id,
            spider_name=spider_config.get(spider_id, {}).get('name', spider_id),
            status='error',
            error_message=str(e),
            last_run_time=datetime.now()
        )
        return jsonify({'error': str(e)}), 500


@app.route('/api/spiders/<spider_id>/stop', methods=['POST'])
def stop_spider(spider_id):
    """Stop a running spider gracefully"""
    try:
        if spider_id not in spider_config:
            return jsonify({'error': 'Spider not found'}), 404

        status = spider_status.get(spider_id, {})
        if status.get('status') != 'running':
            return jsonify({'error': 'Spider is not running'}), 400

        # Get the process and shutdown queue
        process = status.get('process')
        shutdown_queue = status.get('shutdown_queue')
        
        if process and process.is_alive():
            # Send graceful shutdown signal
            if shutdown_queue:
                try:
                    shutdown_queue.put('stop')
                    logger.info(f"Sent graceful shutdown signal to spider: {spider_id}")
                except Exception as e:
                    logger.error(f"Error sending shutdown signal: {e}")
            
            # Wait for graceful shutdown with timeout (30 seconds)
            process.join(timeout=120)
            
            # If still alive after timeout, force terminate
            if process.is_alive():
                logger.warning(f"Spider {spider_id} did not stop gracefully, forcing termination")
                process.terminate()
                process.join(timeout=5)
                if process.is_alive():
                    process.kill()

        # Calculate runtime
        started_at_str = status.get('started_at')
        runtime_seconds = 0
        if started_at_str:
            try:
                started_at = datetime.fromisoformat(started_at_str)
                stopped_at = datetime.now()
                runtime_seconds = int((stopped_at - started_at).total_seconds())
            except:
                pass

        # Update status
        spider_status[spider_id] = {
            'status': 'stopped',
            'stopped_at': datetime.now().isoformat(),
            'last_run': status.get('started_at'),
            'runtime_seconds': runtime_seconds
        }

        # Save to database
        save_spider_status_to_db(
            spider_id=spider_id,
            spider_name=spider_config[spider_id]['name'],
            status='stopped',
            last_run_time=datetime.fromisoformat(status.get('started_at')) if status.get('started_at') else None,
            completed_at=datetime.now(),
            runtime_seconds=runtime_seconds
        )

        logger.info(f"Stopped spider: {spider_id}")
        return jsonify({
            'message': f'Spider {spider_id} stopped',
            'status': 'stopped'
        }), 200

    except Exception as e:
        logger.error(f"Error stopping spider {spider_id}: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/spiders/<spider_id>/status', methods=['GET'])
def get_spider_status(spider_id):
    """Get spider status and runtime"""
    try:
        if spider_id not in spider_config:
            return jsonify({'error': 'Spider not found'}), 404

        # Try to get from database first
        db_status = get_spider_status_from_db(spider_id)

        if db_status:
            runtime_seconds = db_status['runtime_seconds'] or 0

            # If still running, calculate current runtime
            if db_status['status'] == 'running' and db_status['started_at']:
                runtime_seconds = int((datetime.now() - db_status['started_at']).total_seconds())

            return jsonify({
                'spider_id': spider_id,
                'name': spider_config[spider_id]['name'],
                'status': db_status['status'],
                'runtimeSeconds': runtime_seconds,
                'started_at': db_status['started_at'].isoformat() if db_status['started_at'] else None,
                'completed_at': db_status['completed_at'].isoformat() if db_status['completed_at'] else None,
                'last_run': db_status['last_run_time'].isoformat() if db_status['last_run_time'] else None,
                'error': db_status['error_message']
            }), 200
        else:
            # Fallback to memory
            status = spider_status.get(spider_id, {
                'status': 'idle',
                'runtime_seconds': 0
            })

            # Check if process is still alive
            process = status.get('process')
            if process:
                if not process.is_alive() and status.get('status') == 'running':
                    started_at_str = status.get('started_at')
                    runtime_seconds = 0
                    if started_at_str:
                        try:
                            started_at = datetime.fromisoformat(started_at_str)
                            completed_at = datetime.now()
                            runtime_seconds = int((completed_at - started_at).total_seconds())
                        except:
                            pass

                    status['status'] = 'completed'
                    status['runtime_seconds'] = runtime_seconds
                    status['completed_at'] = datetime.now().isoformat()
                    # Clean up process object
                    if 'process' in status:
                        del status['process']
                    spider_status[spider_id] = status

                    # Save to database
                    save_spider_status_to_db(
                        spider_id=spider_id,
                        spider_name=spider_config[spider_id]['name'],
                        status='completed',
                        started_at=datetime.fromisoformat(started_at_str) if started_at_str else None,
                        completed_at=datetime.now(),
                        runtime_seconds=runtime_seconds,
                        last_run_time=datetime.fromisoformat(started_at_str) if started_at_str else None
                    )

            return jsonify({
                'spider_id': spider_id,
                'name': spider_config[spider_id]['name'],
                'status': status.get('status', 'idle'),
                'runtimeSeconds': status.get('runtime_seconds', 0),
                'started_at': status.get('started_at'),
                'completed_at': status.get('completed_at'),
                'last_run': status.get('last_run'),
                'error': status.get('error')
            }), 200

    except Exception as e:
        logger.error(f"Error getting spider status {spider_id}: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'healthy'}), 200


if __name__ == '__main__':
    # Change to the SpiderNew directory
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    # Run the Flask app
    app.run(host='0.0.0.0', port=5000, debug=False)