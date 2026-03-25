import pymysql

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    password='',
    database='ai_gateway',
    charset='utf8'
)
cur = conn.cursor()

# 智谱 GLM 正确的 baseUrl（不含 /chat/completions，代码会自动拼接）
cur.execute("UPDATE channel SET base_url='https://open.bigmodel.cn/api/paas/v4' WHERE provider='glm'")
conn.commit()
print('Updated', cur.rowcount, 'rows')

cur.execute("SELECT id, name, provider, base_url FROM channel WHERE provider='glm'")
for row in cur.fetchall():
    print(row)

conn.close()
