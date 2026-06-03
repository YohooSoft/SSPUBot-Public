from itemadapter import ItemAdapter
import json
from SpiderNew.MSSQLPackage.MSSQLCore import MSSQLCore


class SpidernewPipeline:
    def __init__(self):
        # 按 spider.name 存储每个爬虫的状态：{'file': fileobj, 'postfiles': []}
        self.states = {}

    def open_spider(self, spider):
        filename = f"ds_{spider.name}.jsonl"
        f = open(filename, 'w', encoding='utf-8')
        self.states[spider.name] = {'file': f, 'postfiles': []}

    def close_spider(self, spider):
        state = self.states.pop(spider.name, None)
        filename = f"ds_{spider.name}.jsonl"
        if not state:
            return

        # 导入 MSSQL
        try:
            m = MSSQLCore(host="127.0.0.1", port=1433, database="database", user="user", password="password")
            m.insert_data_from_jsonl(filename, 'posts', schema='dbo')
            m.close()
        except Exception:
            pass

        # 去重并写入 JSON（漂亮格式）
        try:
            unique_files = list(dict.fromkeys(state['postfiles']))
            with open(f"postFiles_{spider.name}.json", 'w', encoding='utf-8') as pf:
                json.dump(unique_files, pf, ensure_ascii=False, indent=2)
        except Exception:
            pass

        # 关闭 ds.jsonl
        f = state.get('file')
        if f:
            try:
                f.flush()
                f.close()
            except Exception:
                pass

        print(f'{spider.name}: 数据已存入 MSSQL，postFiles 已保存。')

    def process_item(self, item, spider):
        state = self.states.get(spider.name)
        adapter = ItemAdapter(item)
        files = adapter.get('postFiles')
        normalized_files = None

        if files:
            # 支持字符串（JSON 或单个 URL）或列表
            if isinstance(files, str):
                try:
                    parsed = json.loads(files)
                    files = parsed if isinstance(parsed, list) else [parsed]
                except Exception:
                    files = [files]
            if isinstance(files, list):
                # 只收集字符串元素（URL）；忽略非字符串项）
                collected = [f for f in files if isinstance(f, str)]
                if state is not None:
                    state['postfiles'].extend(collected)
                normalized_files = collected

        # 把标准化后的 postFiles 写回到要序列化的字典中（以 JSON 字符串保存）
        data = dict(item)
        if normalized_files is not None:
            data['postFiles'] = json.dumps(normalized_files, ensure_ascii=False)
        else:
            data['postFiles'] = None

        # 写入对应爬虫的 ds.jsonl（保证每条记录都包含 postFiles）
        if state and state.get('file'):
            line = json.dumps(data, ensure_ascii=False) + "\n"
            state['file'].write(line)

        return item