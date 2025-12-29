-- Simple script to fix quest configuration
-- Run this in your PostgreSQL database

-- Step 1: Clear old quest progress
DELETE FROM user_quest_progress;

-- Step 2: Clear old quest configs  
DELETE FROM daily_quest_config;

-- Step 3: Reset sequence
SELECT setval('daily_quest_config_quest_id_seq', 1, false);

-- Step 4: Insert 5 quests (20,000 credits each)
INSERT INTO daily_quest_config (description, game_type, target_count, reward_credits) VALUES
    ('Login today', 'ANY', 1, 20000),
    ('Play 5 hands', 'ANY', 5, 20000),
    ('Win 2 matches', 'ANY', 2, 20000),
    ('Play 3 Poker matches', 'POKER', 3, 20000),
    ('Bet 5000 total', 'ANY', 5000, 20000);

-- Verify: Should show 5 quests
SELECT quest_id, description, reward_credits FROM daily_quest_config ORDER BY quest_id;

