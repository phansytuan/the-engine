INSERT INTO users (email, name, password, role, active, created_at, updated_at)
VALUES
    ('admin@taskflow.com', 'Admin User', 'password123', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('john@taskflow.com', 'John Developer', 'password123', 'DEVELOPER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('jane@taskflow.com', 'Jane Manager', 'password123', 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('alice@taskflow.com', 'Alice Engineer', 'password123', 'DEVELOPER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bob@taskflow.com', 'Bob Designer', 'password123', 'DEVELOPER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('carol@taskflow.com', 'Carol PM', 'password123', 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dave@taskflow.com', 'Dave Ops', 'password123', 'DEVELOPER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('eve@taskflow.com', 'Eve QA', 'password123', 'DEVELOPER', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('frank@taskflow.com', 'Frank DevOps', 'password123', 'DEVELOPER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('grace@taskflow.com', 'Grace Lead', 'password123', 'MANAGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tasks (title, description, status, priority, creator_id, assignee_id, created_at, updated_at)
VALUES
    ('Setup Database', 'Configure PostgreSQL database', 'TODO', 'HIGH', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Create API', 'Build REST API endpoints', 'IN_PROGRESS', 'URGENT', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Write Tests', 'Unit and integration tests', 'TODO', 'MEDIUM', 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Design System Setup', 'Configure Tailwind and component library', 'COMPLETED', 'HIGH', 3, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Auth Service', 'Implement JWT authentication flow', 'IN_PROGRESS', 'URGENT', 6, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CI/CD Pipeline', 'Set up GitHub Actions workflows', 'TODO', 'HIGH', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Docker Compose', 'Containerize all services for local dev', 'IN_PROGRESS', 'MEDIUM', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('API Documentation', 'Write OpenAPI/Swagger docs', 'TODO', 'LOW', 6, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dashboard UI', 'Build main dashboard with charts', 'IN_PROGRESS', 'HIGH', 3, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('User Profile Page', 'Profile view and edit functionality', 'TODO', 'MEDIUM', 6, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Notification Service', 'Email and in-app notifications', 'TODO', 'MEDIUM', 10, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Role-Based Access Control', 'Implement RBAC middleware', 'COMPLETED', 'URGENT', 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Search Feature', 'Full-text search with Elasticsearch', 'TODO', 'LOW', 10, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Performance Audit', 'Profile and optimize slow queries', 'IN_PROGRESS', 'HIGH', 3, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Fix Login Bug', 'Session not persisting on Safari', 'IN_PROGRESS', 'URGENT', 6, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Data Export', 'CSV/PDF export for reports', 'TODO', 'LOW', 3, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Onboarding Flow', 'New user walkthrough wizard', 'TODO', 'MEDIUM', 10, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Security Audit', 'Penetration testing and fixes', 'TODO', 'HIGH', 1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mobile Responsiveness', 'Fix layout issues on small screens', 'COMPLETED', 'MEDIUM', 6, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Seed Production Data', 'Prepare realistic data for staging', 'COMPLETED', 'LOW', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);