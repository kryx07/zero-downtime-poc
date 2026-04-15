CREATE TABLE outbox (
    id SERIAL PRIMARY KEY,
    payload TEXT,
    processed BOOLEAN DEFAULT FALSE
);
CREATE TABLE target_table (
    id SERIAL PRIMARY KEY,
    payload TEXT
);
CREATE TABLE app_user (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255)
);

INSERT INTO app_user (id, name) VALUES ('1', 'Alice');
INSERT INTO app_user (id, name) VALUES ('2', 'Bob');
