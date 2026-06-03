class Page:
    def __init__(self, url, title, htmlContent, author="", isPost=False, ReleaseAt=""):
        self.url = url
        self.title = title
        self.author = author
        self.htmlContent = htmlContent
        self.isPost = isPost
        self.ReleaseAt = ReleaseAt

    def __str__(self):
        return f"Post URL: {self.post_url}\nAuthor: {self.author}\nTimestamp: {self.ReleaseAt}\nContent: {self.content}\n"

    def __repr__(self):
        return f"Post({self.post_url}, {self.author}, {self.ReleaseAt})"

    def to_dict(self):
        return {
            "post_url": self.post_url,
            "author": self.author,
            "content": self.content,
            "ReleaseAt": self.ReleaseAt
        }

    def is_post(self):
        return self.isPost
