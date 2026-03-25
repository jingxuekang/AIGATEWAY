import pymysql

conn = pymysql.connect(
    host='localhost', port=3306, user='root', password='',
    database='ai_gateway', charset='utf8'
)
cur = conn.cursor()
cur.execute('SELECT id, name, provider, base_url, models FROM channel ORDER BY id')
print(f'{"id":<5} {"provider":<12} {"base_url":<60} {"models"}')
for r in cur.fetchall():
    print(f'{r[0]:<5} {r[2]:<12} {r[3]:<60} {r[4]}')
conn.close()
