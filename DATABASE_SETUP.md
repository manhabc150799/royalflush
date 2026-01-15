# Hướng dẫn Setup Database

## Bước 1: Tạo Database

Mở PostgreSQL (psql hoặc pgAdmin) và chạy lệnh sau:

```sql
CREATE DATABASE royalflush_game;
```

Hoặc nếu dùng psql command line:
```bash
psql -U postgres -c "CREATE DATABASE royalflush_game;"
```

## Bước 2: Tạo Schema

Sau khi tạo database, kết nối vào database:
```sql
\c royalflush_game
```

Và chạy file schema:
```bash
psql -U postgres -d royalflush_game -f server/src/main/resources/database/schema.sql
```

Hoặc copy nội dung từ `server/src/main/resources/database/schema.sql` và chạy trong pgAdmin.

## Thông tin kết nối

- **Database**: royalflush_game
- **Host**: localhost
- **Port**: 5432
- **Username**: postgres
- **Password**: 12212332

## Kiểm tra

Sau khi setup, bạn có thể kiểm tra bằng cách:
```sql
\c royalflush_game
\dt
```

Sẽ thấy các bảng: users, match_history, game_rooms, room_players


