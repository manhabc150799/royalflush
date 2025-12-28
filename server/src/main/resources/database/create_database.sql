-- Script tạo database mới cho Royal FlushG
-- Chạy script này với user postgres hoặc superuser

-- Tạo database mới (chạy với psql hoặc pgAdmin)
-- psql -U postgres -c "CREATE DATABASE royalflush_game;"

-- Hoặc nếu database đã tồn tại, xóa và tạo lại:
-- DROP DATABASE IF EXISTS royalflush_game;
-- CREATE DATABASE royalflush_game;

-- Sau khi tạo database, kết nối vào database đó:
-- \c royalflush_game

-- Và chạy file schema.sql để tạo tables

