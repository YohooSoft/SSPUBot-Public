# Define here the models for your scraped items
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/items.html

import scrapy


class SpidernewItem(scrapy.Item):
    # define the fields for your item here like:
    # name = scrapy.Field()
    postName = scrapy.Field()
    postReleaseTime = scrapy.Field()
    postSource = scrapy.Field()
    postContent = scrapy.Field()
    postUrl = scrapy.Field()
    postFiles = scrapy.Field()
    postContentUsingMarkdown = scrapy.Field()
    postSimplifiedContent = scrapy.Field()
    postWords = scrapy.Field()
    pass
