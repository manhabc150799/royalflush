-- Royal FlushG Database Schema
-- Database name: royalflush_game
-- Optimized schema for Poker & Tien Len multiplayer card game

-- ============================================
-- 1. USER MANAGEMENT & ECONOMY
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    credits BIGINT DEFAULT 1000 CHECK (credits >= 0),
    current_rank VARCHAR(20) DEFAULT 'IRON',
    total_wins INT DEFAULT 0,
    total_losses INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    last_daily_reward TIMESTAMP
);

-- ============================================
-- 2. RANKING SYSTEM (Logic-based, no separate table)
-- Rank is calculated from credits:
-- IRON: 0 - 100,000
-- BRONZE: 100,001 - 500,000
-- SILVER: 500,001 - 2,000,000
-- GOLD: 2,000,001 - 5,000,000
-- PLATINUM: 5,000,001 - 8,000,000
-- MASTER: 8,000,001 - 10,000,000+
-- ============================================

-- ============================================
-- 3. DAILY QUEST SYSTEM (Resets at 00:00 AM)
-- ============================================

-- Static quest configurations
CREATE TABLE IF NOT EXISTS daily_quest_config (
    quest_id SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    game_type VARCHAR(20) NOT NULL, -- 'POKER', 'TIENLEN', or 'ANY'
    target_count INT NOT NULL,
    reward_credits BIGINT NOT NULL
);

-- User quest progress (resets daily)
CREATE TABLE IF NOT EXISTS user_quest_progress (
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    quest_id INT REFERENCES daily_quest_config(quest_id) ON DELETE CASCADE,
    current_progress INT DEFAULT 0,
    is_claimed BOOLEAN DEFAULT FALSE,
    date_assigned DATE NOT NULL,
    PRIMARY KEY (user_id, quest_id, date_assigned)
);

-- ============================================
-- 4. ROOM & MATCH SYSTEM
-- ============================================

-- Active rooms in lobby
CREATE TABLE IF NOT EXISTS active_rooms (
    room_id SERIAL PRIMARY KEY,
    room_name VARCHAR(100),
    host_user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    game_type VARCHAR(20) NOT NULL, -- 'POKER' or 'TIENLEN'
    max_players INT NOT NULL, -- 5 for Poker, 4 for Tien Len
    current_players INT DEFAULT 1 CHECK (current_players >= 0 AND current_players <= max_players),
    bet_amount BIGINT DEFAULT 0 CHECK (bet_amount >= 0),
    password VARCHAR(50), -- Optional password for private rooms
    status VARCHAR(20) DEFAULT 'WAITING', -- 'WAITING', 'PLAYING', 'FINISHED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP
);

-- Match history (per-user entries)
CREATE TABLE IF NOT EXISTS match_history (
    match_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    game_type VARCHAR(20) NOT NULL, -- 'POKER' or 'TIENLEN'
    match_mode VARCHAR(20) NOT NULL, -- 'MULTIPLAYER' or 'SINGLEPLAYER'
    result VARCHAR(10) NOT NULL, -- 'WIN', 'LOSE', 'DRAW'
    credits_change BIGINT NOT NULL,
    opponent_count INT DEFAULT 1,
    duration_seconds INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Room players (many-to-many relationship)
CREATE TABLE IF NOT EXISTS room_players (
    room_id INT REFERENCES active_rooms(room_id) ON DELETE CASCADE,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    position INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_credits ON users(credits DESC);
CREATE INDEX IF NOT EXISTS idx_user_quest_progress_user_date ON user_quest_progress(user_id, date_assigned);
CREATE INDEX IF NOT EXISTS idx_active_rooms_status ON active_rooms(status);
CREATE INDEX IF NOT EXISTS idx_active_rooms_game_type ON active_rooms(game_type);
CREATE INDEX IF NOT EXISTS idx_match_history_game_type ON match_history(game_type);
CREATE INDEX IF NOT EXISTS idx_match_history_user_id ON match_history(user_id);
CREATE INDEX IF NOT EXISTS idx_match_history_timestamp ON match_history(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_room_players_room_id ON room_players(room_id);
CREATE INDEX IF NOT EXISTS idx_room_players_user_id ON room_players(user_id);

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE users IS 'User accounts with credits and authentication';
COMMENT ON TABLE daily_quest_config IS 'Static daily quest definitions';
COMMENT ON TABLE user_quest_progress IS 'User progress on daily quests (resets at 00:00)';
COMMENT ON TABLE active_rooms IS 'Active game rooms in lobby';
COMMENT ON TABLE match_history IS 'Completed match records for analytics';
COMMENT ON TABLE room_players IS 'Many-to-many relationship between rooms and players';

-- ============================================
-- INITIAL DATA: Daily Quest Configurations
-- ============================================
INSERT INTO daily_quest_config (description, game_type, target_count, reward_credits) VALUES
    ('Win 2 matches in Poker', 'POKER', 2, 500),
    ('Play 5 Tien Len matches', 'TIENLEN', 5, 300),
    ('Win any 3 matches', 'ANY', 3, 1000),
    ('Play 10 matches total', 'ANY', 10, 500)
ON CONFLICT DO NOTHING;
