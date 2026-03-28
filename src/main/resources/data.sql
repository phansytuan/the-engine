INSERT INTO users (email, name, password, role, active, created_at, updated_at)
VALUES
('admin@taskflow.com', 'Admin User', 'password123', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('john@taskflow.com', 'John Developer', 'password123', 'DEVELOPER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('jane@taskflow.com', 'Jane Manager', 'password123', 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tasks (title, description, status, priority, creator_id, assignee_id, created_at, updated_at)
VALUES
('Setup Database', 'Configure PostgreSQL database', 'TODO', 'HIGH', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Create API', 'Build REST API endpoints', 'IN_PROGRESS', 'URGENT', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Write Tests', 'Unit and integration tests', 'TODO', 'MEDIUM', 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
