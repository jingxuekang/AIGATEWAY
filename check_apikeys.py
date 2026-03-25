import pymysql

conn = pymysql.connect(
    host='localhost', port=3306, user='root', password='',
    database='ai_gateway', charset='utf8'
)
cur = conn.cursor()

# 查看 api_key 表结构
cur.execute('DESCRIBE api_key')
print('api_key table:')
for r in cur.fetchall(): print(r)

print()

# 查看 api_key 数据
cur.execute('SELECT id, name, status, allowed_models, remaining_quota FROM api_key LIMIT 10')
print('api_key data:')
for r in cur.fetchall(): print(r)

conn.close()
