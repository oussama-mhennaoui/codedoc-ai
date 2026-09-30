-- Fix the corrupted password hash for demo user
UPDATE users 
SET password = E'$2a$10$Y9sPBdIKVoRzYwJY4KD6XeKGJqfJpqPtOGlvh1bVDHOI0h2g8V.Sa' 
WHERE username = 'demo';
