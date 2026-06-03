import jieba
import scrapy

from SpiderNew.items import SpidernewItem
from SpiderNew.Ai.agents.Tools.transformToMarkdown import transform_html_to_markdown
from SpiderNew.Ai.agents.Tools.generateSimplifiedContent import generate_simplifed_content
from SpiderNew.MSSQLPackage.PostService import PostService
import json

class SpidernewforjwcSpider(scrapy.Spider):
    name = "SpiderNewForJWC"
    allowed_domains = ["jwc.sspu.edu.cn"]
    start_urls = ["https://jwc.sspu.edu.cn/896/list1.htm"]

    def parse(self, response):
        item = SpidernewItem()
        print(f"正在抓取列表页: {response.url}")
        nodesList = response.xpath('//li[contains(@class, "news") and contains(@class, "clearfix")]')
        for node in nodesList:
            href = node.xpath('.//span[@class="news_title"]/a/@href').get()
            date = node.xpath('.//span[@class="news_meta"]/text()').get()

            if href:
                item['postUrl'] = response.urljoin(href)
                item['postReleaseTime'] = date.strip() if date else ""
                yield scrapy.Request(
                    url=item['postUrl'],
                    callback=self.parse_detail,
                    meta={'item': item}
                )

        next_page = response.xpath("//a[@class='next']/@href").get()
        # if '2' in next_page:
        #     return
        if next_page:
            yield scrapy.Request(url=response.urljoin(next_page), callback=self.parse)

    def parse_detail(self, response):
        m = PostService(host="127.0.0.1", port=1433, database="database", user="user", password="password")
        if m.isThisPostExist(response.url):
            return

        item = response.meta['item']
        print(f"正在抓取详情页: {response.url}")

        name = response.xpath('//h1[@class="arti_title"]/text()').get()
        item['postName'] = name.strip() if name else ""

        src = response.xpath('//span[@class="arti_views"]/text()').get() or ""
        item['postSource'] = src[3:].strip()  # 使用切片代替 substring，并处理 None

        allThings = {
            "content_nodes": response.xpath('//div[@class="WordSection1"]').getall(),
            "files": response.xpath('//div[@class="read"]')
        }
        if allThings.get("content_nodes"):
            item['postContent'] = allThings["content_nodes"]
            item['postContentUsingMarkdown'] = transform_html_to_markdown(''.join(allThings["content_nodes"]))
            item['postSimplifiedContent'] = generate_simplifed_content(''.join(allThings["content_nodes"]))
            item['postWords'] = json.dumps(list(jieba.cut(item['postSimplifiedContent'])), ensure_ascii=False)
        elif allThings.get("files"):
            item['postContent'] = allThings["files"].getall()
            listAll = ([response.urljoin(src) for src in response.xpath('//img/@src').getall() if
                        '/_visitcount' not in src] if response.xpath('//img/@src').getall() else []) + (
                          [response.xpath('//div[@class="wp_pdf_player"]/@pdfsrc').get().strip()] if response.xpath(
                              '//div[@class="wp_pdf_player"]').get() else []) + (
                          [allThings["files"].xpath('.//div[@class="wp_articlecontent"]/p/a/@href').get().strip()] if
                          allThings["files"].xpath('.//div[@class="wp_articlecontent"]/p/a').get() else [])
            item['postFiles'] = [
                response.urljoin(file_url) for file_url in listAll
            ]
            item['postContentUsingMarkdown'] = transform_html_to_markdown(''.join(allThings["files"].getall()))
            item['postSimplifiedContent'] = generate_simplifed_content(''.join(allThings["files"].getall()))
            item['postWords'] = json.dumps(list(jieba.cut(item['postSimplifiedContent'])), ensure_ascii=False)
        else:
            item['postContent'] = "内容未找到"
            item['postFiles'] = []

        yield item
