-- Bot Leaderboard Metrics Table
-- Stores pre-calculated metrics for fast leaderboard queries
-- Composite primary key allows one bot to have both DRY_RUN and HISTORICAL metrics

CREATE TABLE bot_leaderboard_metrics (
    bot_id VARCHAR(255) NOT NULL,
    data_source VARCHAR(20) NOT NULL,  -- 'DRY_RUN' or 'HISTORICAL'
    cagr DOUBLE PRECISION NOT NULL,
    sharpe DOUBLE PRECISION NOT NULL,
    max_drawdown DOUBLE PRECISION NOT NULL,
    sample_days BIGINT NOT NULL,
    last_calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bot_id, data_source)  -- Composite Primary Key
);

-- Indexes for fast filtering + sorting by different metrics
CREATE INDEX idx_leaderboard_dry_run_cagr ON bot_leaderboard_metrics(data_source, cagr DESC) 
    WHERE data_source = 'DRY_RUN';
CREATE INDEX idx_leaderboard_dry_run_sharpe ON bot_leaderboard_metrics(data_source, sharpe DESC) 
    WHERE data_source = 'DRY_RUN';
CREATE INDEX idx_leaderboard_historical_cagr ON bot_leaderboard_metrics(data_source, cagr DESC) 
    WHERE data_source = 'HISTORICAL';
CREATE INDEX idx_leaderboard_historical_sharpe ON bot_leaderboard_metrics(data_source, sharpe DESC) 
    WHERE data_source = 'HISTORICAL';