from SpiderNew.MSSQLPackage.MSSQLCore import MSSQLCore

class PostService(MSSQLCore):

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

    def isThisPostExist(self, postUrl) -> bool:
        query = "SELECT COUNT(1) FROM dbo.posts WHERE postUrl = ?"
        params = [postUrl]
        sanitized_params = self._sanitize_params(params)

        conn = self.connect_pyodbc()
        cursor = conn.cursor()
        try:
            cursor.execute(query, sanitized_params)
            result = cursor.fetchone()
            return bool(result and result[0] > 0)
        finally:
            cursor.close()
            # 如果 connect_pyodbc 每次返回新连接，按需关闭：
            # conn.close()