INSERT INTO users (username, email, password, role)
VALUES ('demo', 'demo@example.com', '$2a$10$70JkTkQ/Y.r6osjISMhcQeHaYpihHsMhBpEJYPkILCx.gqT8CfG9O', 'USER')
ON CONFLICT (username) DO NOTHING;

INSERT INTO projects (owner_id, name, description, language)
SELECT id, 'Demo Project', 'A sample project for demonstration', 'Java'
FROM users WHERE username = 'demo'
ON CONFLICT DO NOTHING;

INSERT INTO source_files (project_id, filename, content, language, size_bytes, checksum)
SELECT p.id, 'HelloWorld.java', 'public class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println("Hello, World!");\n    }\n}', 'Java', 114, '0fa1a94ab8482d921b72e59174df8344'
FROM projects p
JOIN users u ON p.owner_id = u.id
WHERE u.username = 'demo' AND p.name = 'Demo Project'
ON CONFLICT (project_id, filename) DO NOTHING;
