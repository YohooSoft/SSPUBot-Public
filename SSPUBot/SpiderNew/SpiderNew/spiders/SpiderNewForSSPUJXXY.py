import scrapy
from SpiderNew.items import SpidernewItem
from SpiderNew.Ai.agents.Tools.transformToMarkdown import transform_html_to_markdown
from SpiderNew.Ai.agents.Tools.generateSimplifiedContent import generate_simplifed_content
from SpiderNew.MSSQLPackage.PostService import PostService
import jieba
import json

class SpidernewforsspujxxySpider(scrapy.Spider):
    name = "SpiderNewForSSPUJXXY"
    allowed_domains = ["jxxy.sspu.edu.cn"]
    start_urls = [
        "https://jxxy.sspu.edu.cn/2075/list.htm",
        "https://jxxy.sspu.edu.cn/xshd/list.htm",
        "https://jxxy.sspu.edu.cn/2084/list.htm",
        "https://jxxy.sspu.edu.cn/2085/list.htm",
        "https://jxxy.sspu.edu.cn/2086/list.htm",
        "https://jxxy.sspu.edu.cn/2059/list.htm",
        "https://jxxy.sspu.edu.cn/2062/list.htm",
        "https://jxxy.sspu.edu.cn/2063/list.htm",
        "https://jxxy.sspu.edu.cn/2064/list.htm",
        "https://jxxy.sspu.edu.cn/2065/list.htm",
        "https://jxxy.sspu.edu.cn/2066/list.htm",
        "https://jxxy.sspu.edu.cn/2068/list.htm",
        "https://jxxy.sspu.edu.cn/2069/list.htm",
        "https://jxxy.sspu.edu.cn/2070/list.htm"
    ]

    def parse(self, response):
        # 列表页：过滤掉 javascript: / # 等无效链接
        for href in response.xpath('//div[@class="list-1"]/a[@class="item"]/@href').getall():
            if not href:
                continue
            href = href.strip()
            if href.lower().startswith('javascript') or href.startswith('#'):
                continue
            yield response.follow(
                url=response.urljoin(href),
                callback=self.parse_detail
            )

        next_page = response.xpath("//a[@class='next']/@href").get()
        if next_page:
            next_page = next_page.strip()
            if not next_page.lower().startswith('javascript') and not next_page.startswith('#'):
                yield response.follow(
                    url=response.urljoin(next_page),
                    callback=self.parse
                )

    def parse_detail(self, response):
        m = PostService(host="127.0.0.1", port=1433, database="database", user="user", password="password")
        if m.isThisPostExist(response.url):
            return

        item = SpidernewItem()

        print(f"正在抓取详情页: {response.url}")

        title = response.xpath('//div[@class="title"]/text()').get()
        item['postName'] = title.strip() if title else ''

        time_text = response.xpath('//div[@class="time-2 pull-right"]/text()').get()
        postReleaseTime = ''
        if time_text:
            time_text = time_text.strip()
            if time_text.startswith('发布时间：'):
                rest = time_text[len('发布时间：'):]
                space_idx = rest.find(' ')
                postReleaseTime = rest if space_idx == -1 else rest[:space_idx]
            else:
                postReleaseTime = time_text
        item['postReleaseTime'] = postReleaseTime

        item['postUrl'] = response.url
        item['postSource'] = "计算机与信息工程学院"

        contents = response.xpath('//div[@class="wp_articlecontent"]').getall()
        item['postContent'] = contents

        imgs = response.xpath('//img/@src').getall()
        files = response.xpath('//a').getall()
        item['postFiles'] = [response.urljoin(src) for src in imgs if
                             src and '/_visitcount' not in src and '/_upload' in src]
        item['postFiles'] += [response.urljoin(file_url) for file_url in
                              files
                              if file_url and file_url.strip().lower().endswith(('.pdf', '.doc', '.docx', '.xls', '.xlsx', '.zip', '.rar'))]

        full_html = ''.join(contents)
        item['postContentUsingMarkdown'] = transform_html_to_markdown(full_html)
        item['postSimplifiedContent'] = generate_simplifed_content(full_html)
        item['postWords'] = json.dumps(list(jieba.cut(item['postSimplifiedContent'])), ensure_ascii=False)

        yield item
