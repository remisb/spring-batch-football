CREATE TABLE IF NOT EXISTS player (
    player_id VARCHAR(30),
    last_name VARCHAR(30),
    first_name VARCHAR(30),
    position VARCHAR(30),
    birth_year INT(11),
    debut_year INT(11)
--     PRIMARY KEY (player_id)
);

CREATE TABLE IF NOT EXISTS game (
    player_id VARCHAR(30),
    year INT(11),
    team VARCHAR(30),
    week INT(11),
    opponent VARCHAR(30),
    completes INT(11),
    attempts INT(11),
    passing_yards INT(11),
    passing_td INT(11),
    interceptions INT(11),
    rushes INT(11),
    rush_yards INT(11),
    receptions INT(11),
    reception_yards INT(11),
    total_td INT(11)
--     PRIMARY KEY (player_id)
);

CREATE TABLE IF NOT EXISTS player_summary (
    id VARCHAR(30),
    year_no INT(11),
    completes INT(11),
    attempts INT(11),
    passing_yards INT(11),
    passing_td INT(11),
    interceptions INT(11),
    rushes INT(11),
    rush_yards INT(11),
    receptions INT(11),
    reception_yards INT(11),
    total_td INT(11)
--     PRIMARY KEY (player_id)
);
