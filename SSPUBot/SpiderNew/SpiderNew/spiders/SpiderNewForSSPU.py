import scrapy
from SpiderNew.items import SpidernewItem
from SpiderNew.Ai.agents.Tools.transformToMarkdown import transform_html_to_markdown
from SpiderNew.Ai.agents.Tools.generateSimplifiedContent import generate_simplifed_content
from SpiderNew.MSSQLPackage.PostService import PostService
import jieba
import json

class SpidernewforsspuSpider(scrapy.Spider):
    name = "SpiderNewForSSPU"
    allowed_domains = ["sspu.edu.cn"]
    start_urls = ["https://sspu.edu.cn/2964/list.htm"]

    def parse(self, response):
        item = SpidernewItem()
        # 列表页示例：跟踪链接到详情页
        for href in response.xpath('//a[@class="news_box1 clearfix"]/@href').getall():
            yield response.follow(
                url=response.urljoin(href),
                callback=self.parse_detail
            )

        next_page = response.xpath("//a[@class='next']/@href").get()
        if next_page:
            yield response.follow(
                url=response.urljoin(next_page),
                callback=self.parse
            )

    def parse_detail(self, response):
        m = PostService(host="127.0.0.1", port=1433, database="database", user="user", password="password")
        if m.isThisPostExist(response.url):
            return

        # 详情页示例：提取标题和链接
        item = SpidernewItem()

        print(f"正在抓取详情页: {response.url}")

        item['postName'] = response.xpath('//h1[@class="arti_title"]/text()').get().strip()
        item['postUrl'] = response.url
        item['postReleaseTime'] = response.xpath('//span[@class="arti_update"]/text()').get().strip()[len("发布时间："):]
        item['postSource'] = response.xpath('//span[@class="arti_gg"]/text()').get().strip()[3:]
        item['postContent'] = response.xpath('//div[@class="WordSection1"]').getall() or response.xpath('//div[@class="wp_articlecontent"]').getall()
        item['postFiles'] = [ response.urljoin(src) for src in response.xpath('//img/@src').getall() if '/_visitcount' not in src and '/_upload' in src ]
        item['postContentUsingMarkdown'] = transform_html_to_markdown(''.join(item['postContent']))
        item['postSimplifiedContent'] = generate_simplifed_content(''.join(item['postContent']))
        item['postWords'] = json.dumps(list(jieba.cut(item['postSimplifiedContent'])), ensure_ascii=False)

        yield item