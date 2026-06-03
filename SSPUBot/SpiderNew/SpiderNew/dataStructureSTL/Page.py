class Page:
    def __init__(self, postName, postReleaseTime, postSource, postContent, postUrl, postFiles, postContentUsingMarkdown, postSimplifiedContent):
        self.postName = postName
        self.postReleaseTime = postReleaseTime
        self.postSource = postSource
        self.postContent = postContent
        self.postUrl = postUrl
        self.postFiles = postFiles
        self.postContentUsingMarkdown = postContentUsingMarkdown
        self.postSimplifiedContent = postSimplifiedContent

    def __str__(self):
        return f"""Post Name: {self.postName}\n
Author: {self.postSource}\n
Post Release Time: {self.postReleaseTime}\n
Content: {self.postContent}\n
Files: {self.postFiles}
Post Content (Markdown): {self.postContentUsingMarkdown}\n
Post Content (Simplified): {self.postSimplifiedContent}
Post URL: {self.postUrl}
"""

    def __repr__(self):
        return f"""Post(
Name: {self.postName},
Author: {self.postSource},
ReleaseAt: {self.postReleaseTime},
Content: {self.postContent},
Files: {self.postFiles},
Content (Markdown): {self.postContentUsingMarkdown},
Content (Simplified): {self.postSimplifiedContent},
URL: {self.postUrl}
)"""

    def to_dict(self):
        return {
            "post_name": self.postName,
            "post_release_time": self.postReleaseTime,
            "post_source": self.postSource,
            "post_content": self.postContent,
            "post_url": self.postUrl,
            "post_files": self.postFiles,
            "post_content_markdown": self.postContentUsingMarkdown,
            "post_content_simplified": self.postSimplifiedContent
        }

    def is_post(self):
        return self.isPost
