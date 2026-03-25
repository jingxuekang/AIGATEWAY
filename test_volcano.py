import urllib.request
import json
import pymysql

conn = pymysql.connect(host='localhost', port=3306, user='root', password='', database='ai_gateway', charset='utf8')
cur = conn.cursor()
cur.execute("SELECT base_url, api_key FROM channel WHERE provider='volcano'")
row = cur.fetchone()
conn.close()

base_url, api_key = row[0], row[1]

url = f'{base_url}/responses'
body = {
    'model': 'doubao-seed-2-0-pro-260215',
    'input': [
        {
            'role': 'user',
            'content': [
                {'type': 'input_image', 'image_url': 'https://ark-project.tos-cn-beijing.volces.com/images/view.jpeg'},
                {'type': 'input_text', 'text': '图片里有什么？'}
            ]
        }
    ]
}

req = urllib.request.Request(
    url,
    data=json.dumps(body).encode('utf-8'),
    headers={'Content-Type': 'application/json', 'Authorization': f'Bearer {api_key}'},
    method='POST'
)
try:
    with urllib.request.urlopen(req, timeout=20) as resp:
        print('Status:', resp.status)
        print(resp.read().decode('utf-8')[:800])
except urllib.error.HTTPError as e:
    print('HTTP Error:', e.code)
    print(e.read().decode('utf-8'))
