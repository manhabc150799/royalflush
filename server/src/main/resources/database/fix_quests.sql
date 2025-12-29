-- Fix Daily Quests Migration Script
-- Run this script to reset quest configuration to exactly 5 quests

-- Step 1: Delete existing user quest progress (required due to foreign key)
DELETE FROM user_quest_progress;

-- Step 2: Delete existing quest configs
DELETE FROM daily_quest_config;

-- Step 3: Reset the sequence to start from 1
ALTER SEQUENCE IF EXISTS daily_quest_config_quest_id_seq RESTART WITH 1;

-- Step 4: Insert the 5 daily quests (20,000 credits each)
INSERT INTO daily_quest_config (description, game_type, target_count, reward_credits) VALUES
    ('Login today', 'ANY', 1, 20000),
    ('Play 5 hands', 'ANY', 5, 20000),
    ('Win 2 matches', 'ANY', 2, 20000),
    ('Play 3 Poker matches', 'POKER', 3, 20000),
    ('Bet 5000 total', 'ANY', 5000, 20000);

-- Verify: Should return 5 rows
SELECT quest_id, description, reward_credits FROM daily_quest_config ORDER BY quest_id;

