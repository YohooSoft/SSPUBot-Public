# python
from urllib.parse import quote_plus

import pyodbc
import pymssql
from sqlalchemy import create_engine, text


class MSSQLCore:
    """
    简单的 MSSQL 连接封装，支持 pyodbc、pymssql、sqlalchemy。
    参数:
      host, port, database, user, password, driver (用于 pyodbc)
    """

    def __init__(self, host, database, user, password, port=1433, driver="ODBC Driver 17 for SQL Server"):
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.driver = driver
        self._pyodbc_conn = None
        self._pymssql_conn = None
        self._engine = None

    # pyodbc 连接
    def connect_pyodbc(self):
        conn_str = (
            f"DRIVER={{{self.driver}}};"
            f"SERVER={self.host},{self.port};"
            f"DATABASE={self.database};"
            f"UID={self.user};PWD={self.password}"
        )
        self._pyodbc_conn = pyodbc.connect(conn_str, autocommit=False)
        return self._pyodbc_conn

    # pymssql 连接
    def connect_pymssql(self):
        self._pymssql_conn = pymssql.connect(
            server=self.host, user=self.user, password=self.password,
            database=self.database, port=self.port
        )
        return self._pymssql_conn

    # sqlalchemy engine（基于 pyodbc）
    def connect_sqlalchemy(self):
        quoted = quote_plus(
            f"DRIVER={{{self.driver}}};SERVER={self.host},{self.port};DATABASE={self.database};UID={self.user};PWD={self.password}")
        url = f"mssql+pyodbc:///?odbc_connect={quoted}"
        self._engine = create_engine(url)
        return self._engine

    # 执行查询（pyodbc / pymssql / sqlalchemy 均可）
    def execute_query_pyodbc(self, sql, params=None):
        if self._pyodbc_conn is None:
            self.connect_pyodbc()
        cur = self._pyodbc_conn.cursor()
        cur.execute(sql, params or ())
        rows = cur.fetchall()
        cur.close()
        return rows

    def execute_non_query_pyodbc(self, sql, params=None):
        if self._pyodbc_conn is None:
            self.connect_pyodbc()
        cur = self._pyodbc_conn.cursor()
        cur.execute(sql, params or ())
        self._pyodbc_conn.commit()
        cur.close()

    def execute_query_sqlalchemy(self, sql, params=None):
        if self._engine is None:
            self.connect_sqlalchemy()
        with self._engine.connect() as conn:
            result = conn.execute(text(sql), params or {})
            return result.fetchall()

    def close(self):
        try:
            if self._pyodbc_conn:
                self._pyodbc_conn.close()
        except Exception:
            pass
        try:
            if self._pymssql_conn:
                self._pymssql_conn.close()
        except Exception:
            pass
        try:
            if self._engine:
                self._engine.dispose()
        except Exception:
            pass


if __name__ == "__main__":
    # 使用示例（请替换为真实连接信息）
    m = MSSQLCore(host="127.0.0.1", port=1433, database="database", user="user", password="password")
    # rows = m.execute_query_sqlalchemy("SELECT TOP 10 * FROM dbo.YourTable")
    m.execute_non_query_pyodbc("CREATE TABLE TestTable (ID INT PRIMARY KEY, Name NVARCHAR(50))")
    # for r in rows:
    #     print(r)
    m.close()
