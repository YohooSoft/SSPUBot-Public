from urllib.parse import quote_plus
import pyodbc
import pymssql
from sqlalchemy import create_engine, text
import json
import datetime


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

    def _quote_identifier(self, name):
        # 用方括号转义标识符，并对内部 ] 进行转义
        if not name:
            return name
        return f"[{str(name).replace(']', ']]')}]"

    # 参数清洗，确保都是标量类型或可序列化为字符串
    def _sanitize_param_value(self, v):
        if v is None:
            return None
        if isinstance(v, (dict, list, tuple)):
            return json.dumps(v, ensure_ascii=False)
        if isinstance(v, (datetime.date, datetime.datetime)):
            return v.isoformat()
        if isinstance(v, bool):
            return int(v)
        if isinstance(v, (int, float, str, bytes, bytearray)):
            return v
        # 兜底：将其他类型转为字符串
        return str(v)

    def _sanitize_params(self, params):
        return [self._sanitize_param_value(p) for p in (params or [])]

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
        sanitized = tuple(self._sanitize_params(params))
        cur.execute(sql, sanitized)
        rows = cur.fetchall()
        cur.close()
        return rows

    def execute_non_query_pyodbc(self, sql, params=None):
        if self._pyodbc_conn is None:
            self.connect_pyodbc()
        cur = self._pyodbc_conn.cursor()
        sanitized = tuple(self._sanitize_params(params))
        try:
            cur.execute(sql, sanitized)
            self._pyodbc_conn.commit()
        except Exception as e:
            # 抛出更有用的错误信息，包含 SQL 与 参数，便于定位问题
            err_msg = f"执行 SQL 失败: {e}\nSQL: {sql}\nPARAMS: {sanitized}"
            cur.close()
            raise RuntimeError(err_msg) from e
        finally:
            try:
                cur.close()
            except Exception:
                pass

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

    def _table_exists(self, table_name, schema=None):
        if schema:
            sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
            rows = self.execute_query_pyodbc(sql, [schema, table_name])
        else:
            sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?"
            rows = self.execute_query_pyodbc(sql, [table_name])
        return len(rows) > 0

    def insert_data_from_jsonl(self, jsonl_file, table_name, schema=None):
        with open(jsonl_file, 'r', encoding='utf-8') as f:
            for line in f:
                data = json.loads(line)
                self.insert_data(data, table_name, schema)

    # python
    def insert_data(self, data, table_name, schema=None):
        # 检查表是否存在，若不存在则使用当前记录作为样本自动创建表
        if schema:
            exists = self._table_exists(table_name, schema=schema)
        else:
            exists = self._table_exists(table_name, schema=None)

        if not exists:
            # 使用当前 data 作为样本创建表（如果需要主键，可改为传入 primary_key 参数并转发）
            self.create_table_from_sample(data, table_name, schema=schema)

        # 转义列名并构建 SQL
        cols = ','.join(self._quote_identifier(k) for k in data.keys())
        placeholders = ','.join(['?' for _ in data.values()])
        if schema:
            tbl = f"{self._quote_identifier(schema)}.{self._quote_identifier(table_name)}"
        else:
            tbl = self._quote_identifier(table_name)
        sql = f"INSERT INTO {tbl} ({cols}) VALUES ({placeholders})"

        # 执行并在异常时输出 SQL 与参数（execute_non_query_pyodbc 已处理）
        self.execute_non_query_pyodbc(sql, list(data.values()))

    def _infer_sql_type(self, v):
        if v is None:
            return "NVARCHAR(MAX)"
        if isinstance(v, bool):
            return "BIT"
        if isinstance(v, int) and not isinstance(v, bool):
            return "BIGINT"
        if isinstance(v, float):
            return "FLOAT"
        if isinstance(v, (bytes, bytearray)):
            return "VARBINARY(MAX)"
        if isinstance(v, (datetime.date, datetime.datetime)):
            return "DATETIME2"
        return "NVARCHAR(MAX)"

    def create_table_from_sample(self, sample_data, table_name, schema=None, primary_key=None):
        """
        根据 sample_data（dict）推断列并创建表（若不存在）。
        primary_key: 可选，指定列名作为主键（主键列将被设为 NOT NULL）。
        """
        if self._table_exists(table_name, schema=schema):
            return

        cols_defs = []
        # 先处理样本中的列
        for k, v in sample_data.items():
            col_name = self._quote_identifier(k)
            sql_type = self._infer_sql_type(v)
            nullability = "NOT NULL" if primary_key and k == primary_key else "NULL"
            cols_defs.append(f"{col_name} {sql_type} {nullability}")

        # 如果指定了 primary_key 且不在 sample_data 中，则补充该列（默认 NVARCHAR(MAX) NOT NULL）
        if primary_key and primary_key not in sample_data:
            pk_ident = self._quote_identifier(primary_key)
            cols_defs.append(f"{pk_ident} NVARCHAR(MAX) NOT NULL")

        # 添加主键约束
        if primary_key:
            pk_ident = self._quote_identifier(primary_key)
            constraint_name = self._quote_identifier(f"{table_name}_pk")
            cols_defs.append(f"CONSTRAINT {constraint_name} PRIMARY KEY ({pk_ident})")

        cols_sql = ", ".join(cols_defs)
        if schema:
            tbl = f"{self._quote_identifier(schema)}.{self._quote_identifier(table_name)}"
        else:
            tbl = self._quote_identifier(table_name)

        create_sql = f"CREATE TABLE {tbl} ({cols_sql})"
        self.execute_non_query_pyodbc(create_sql)

if __name__ == "__main__":
    filename = f"ds_SpiderNewForJWC.jsonl"

    # 导入 MSSQL
    try:
        m = MSSQLCore(host="127.0.0.1", port=1433, database="database", user="user", password="password")
        m.insert_data_from_jsonl(filename, 'posts', schema='dbo')
        m.close()
    except Exception as e:
        print(e)

    # 去重并写入 JSON（漂亮格式）
    try:
        unique_files = list(dict.fromkeys(state['postfiles']))
        with open(f"postFiles_SpiderNewForJWC.json", 'w', encoding='utf-8') as pf:
            json.dump(unique_files, pf, ensure_ascii=False, indent=2)
    except Exception:
        pass
