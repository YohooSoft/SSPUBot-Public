from scrapy.crawler import CrawlerProcess
from scrapy.utils.project import get_project_settings

process = CrawlerProcess(get_project_settings())
process.crawl('SpiderNewForJWC')
process.crawl('SpiderNewForSSPU')
process.crawl("SpiderNewForSSPUPe2016")
process.crawl("SpiderNewForSSPUJXXY")
process.start()  # 阻塞直到所有爬虫完成