import bcrypt
import subprocess

pwd = bcrypt.hashpw(b'admin123', bcrypt.gensalt(rounds=10)).decode()
print('Generated hash:', pwd)

sql = "UPDATE admin_user SET password='" + pwd + "' WHERE username='admin';"

cmd = [
    'C:/Program Files/MySQL/MySQL Server 9.5/bin/mysql.exe',
    '-u', 'root',
    '--password=',
    'ai_gateway',
    '-e', sql
]
result = subprocess.run(cmd, capture_output=True, text=True)
print('stdout:', result.stdout)
print('stderr:', result.stderr)
print('returncode:', result.returncode)
