CREATE TABLE workout_metrics (

    id BIGSERIAL PRIMARY KEY,

    workout_type VARCHAR(255) NOT NULL,

    intensity INTEGER NOT NULL,

    calories_burned INTEGER NOT NULL,

    started_at TIMESTAMP WITH TIME ZONE NOT NULL,

    ended_at TIMESTAMP WITH TIME ZONE NOT NULL

);