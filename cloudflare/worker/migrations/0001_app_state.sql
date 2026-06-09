CREATE TABLE IF NOT EXISTS app_state (
  user_id TEXT PRIMARY KEY,
  exercises_json TEXT NOT NULL,
  workouts_json TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
