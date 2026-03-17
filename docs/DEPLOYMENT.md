# 部署指南

## 环境准备

### 1. 安装 JDK 17

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# 验证安装
java -version
```

### 2. 安装 MySQL 8.0

```bash
# Ubuntu/Debian
sudo apt install mysql-server

# macOS
brew install mysql

# 启动 MySQL
sudo systemctl start mysql  # Linux
brew services start mysql   # macOS
```

### 3. 安装 Elasticsearch 8.x

```bash
# 使用 Docker 快速启动
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.13.0
```

### 4. 安装 Redis

```bash
# Ubuntu/Debian
sudo apt install redis-server

# macOS
brew install redis

# 启动 Redis
sudo systemctl start redis  # Linux
brew services start redis   # macOS
```

### 5. 安装 Node.js 18+

```bash
# 使用 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18

# 验证安装
node -v
npm -v
```

## 数据库初始化

### 1. 创建数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE ai_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 导入表结构

```bash
mysql -u root -p ai_gateway < docs/database/schema.sql
```

### 3. 验证

```sql
USE ai_gateway;
SHOW TABLES;
SELECT * FROM model;
```

## 后端部署

### 开发环境

#### 1. 启动网关服务

```bash
cd gateway-core
mvn clean install
mvn spring-boot:run
```

服务地址: http://localhost:8080

#### 2. 启动管理服务

```bash
cd gateway-admin
mvn clean install
mvn spring-boot:run
```

服务地址: http://localhost:8081

### 生产环境

#### 1. 打包

```bash
# 打包所有模块
mvn clean package -DskipTests

# 或分别打包
cd gateway-core && mvn clean package -DskipTests
cd gateway-admin && mvn clean package -DskipTests
```

#### 2. 运行

```bash
# 网关服务
java -jar gateway-core/target/gateway-core-1.0.0.jar

# 管理服务
java -jar gateway-admin/target/gateway-admin-1.0.0.jar
```

#### 3. 使用 systemd 管理服务

创建 `/etc/systemd/system/ai-gateway-core.service`:

```ini
[Unit]
Description=AI Gateway Core Service
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/ai-gateway
ExecStart=/usr/bin/java -jar /opt/ai-gateway/gateway-core-1.0.0.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

启动服务:

```bash
sudo systemctl daemon-reload
sudo systemctl start ai-gateway-core
sudo systemctl enable ai-gateway-core
sudo systemctl status ai-gateway-core
```

## 前端部署

### 开发环境

```bash
cd frontend
npm install
npm run dev
```

访问: http://localhost:3000

### 生产环境

#### 1. 构建

```bash
cd frontend
npm install
npm run build
```

构建产物在 `frontend/dist` 目录。

#### 2. 使用 Nginx 部署

安装 Nginx:

```bash
sudo apt install nginx  # Ubuntu/Debian
brew install nginx      # macOS
```

配置 `/etc/nginx/sites-available/ai-gateway`:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /var/www/ai-gateway/frontend/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 代理后端 API
    location /api/ {
        proxy_pass http://localhost:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /v1/ {
        proxy_pass http://localhost:8080/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

启用配置:

```bash
sudo ln -s /etc/nginx/sites-available/ai-gateway /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## Docker 部署

### 1. 创建 Dockerfile

`gateway-core/Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/gateway-core-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 创建 docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ai_gateway
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./docs/database/schema.sql:/docker-entrypoint-initdb.d/schema.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  elasticsearch:
    image: elasticsearch:8.13.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  gateway-core:
    build: ./gateway-core
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - elasticsearch

  gateway-admin:
    build: ./gateway-admin
    ports:
      - "8081:8081"
    depends_on:
      - mysql
      - elasticsearch

volumes:
  mysql-data:
```

### 3. 启动

```bash
docker-compose up -d
```

## 配置说明

### 环境变量

可以通过环境变量覆盖配置:

```bash
# 数据库配置
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ai_gateway
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password

# Redis 配置
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379

# Elasticsearch 配置
export SPRING_ELASTICSEARCH_URIS=http://localhost:9200
```

### 配置文件

生产环境建议使用 `application-prod.yml`:

```yaml
spring:
  profiles:
    active: prod
  
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
```

启动时指定配置:

```bash
java -jar app.jar --spring.profiles.active=prod
```

## 监控与日志

### 查看日志

```bash
# 实时查看日志
tail -f logs/application.log

# 查看错误日志
grep ERROR logs/application.log
```

### 健康检查

```bash
# 网关服务
curl http://localhost:8080/actuator/health

# 管理服务
curl http://localhost:8081/actuator/health
```

## 故障排查

### 常见问题

1. **数据库连接失败**
   - 检查 MySQL 是否启动
   - 验证数据库连接配置
   - 检查防火墙设置

2. **Elasticsearch 连接失败**
   - 确认 ES 服务运行
   - 检查端口 9200 是否开放
   - 验证 ES 配置

3. **前端无法访问后端**
   - 检查 Nginx 代理配置
   - 验证后端服务是否启动
   - 检查 CORS 配置

## 性能优化

### JVM 参数

```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar app.jar
```

### 数据库优化

```sql
-- 添加索引
CREATE INDEX idx_create_time ON api_key(create_time);
CREATE INDEX idx_model_provider ON model(provider, status);
```

## 备份与恢复

### 数据库备份

```bash
# 备份
mysqldump -u root -p ai_gateway > backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p ai_gateway < backup_20240312.sql
```

### Elasticsearch 备份

```bash
# 创建快照仓库
curl -X PUT "localhost:9200/_snapshot/backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/backup/elasticsearch"
  }
}
'

# 创建快照
curl -X PUT "localhost:9200/_snapshot/backup/snapshot_1"
```
