-- Fix VIEW_CHILD_ATTENDANCE and VIEW_DEPARTMENT_ATTENDANCE permissions
-- Add these permissions to USER role (id=4) and potentially other roles

-- First, check what role IDs and permission IDs we have
SELECT 'Existing Roles:' as info;
SELECT id, name, description FROM role ORDER BY id;

SELECT 'Existing Permissions:' as info;
SELECT id, name, description FROM permission 
WHERE name IN ('VIEW_CHILD_ATTENDANCE', 'VIEW_DEPARTMENT_ATTENDANCE', 'VIEW_ALL_ATTENDANCE')
ORDER BY name;

SELECT 'Current Role Permissions:' as info;
SELECT rp.id, r.name as role_name, p.name as permission_name 
FROM role_permission rp
JOIN role r ON rp.role_id = r.id
JOIN permission p ON rp.permission_id = p.id
WHERE p.name IN ('VIEW_CHILD_ATTENDANCE', 'VIEW_DEPARTMENT_ATTENDANCE', 'VIEW_ALL_ATTENDANCE')
ORDER BY r.name, p.name;

-- Fix attendance permissions according to proper hierarchy

-- 1. Add VIEW_DEPARTMENT_ATTENDANCE to USER role 
--    (Users should see their own department colleagues)
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'USER' AND p.name = 'VIEW_DEPARTMENT_ATTENDANCE'
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 
    WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
);

-- 2. Add VIEW_CHILD_ATTENDANCE to managerial roles
--    (Managers should see their child departments)
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name IN ('MANAGER', 'TEAM_LEAD', 'DEPARTMENT_HEAD', 'SUPERVISOR') 
AND p.name = 'VIEW_CHILD_ATTENDANCE'
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 
    WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
);

-- 3. Add both permissions to ADMIN roles
--    (Admins should have both department and child access)
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name IN ('ADMIN', 'HR_MANAGER', 'HR_ADMIN') 
AND p.name IN ('VIEW_CHILD_ATTENDANCE', 'VIEW_DEPARTMENT_ATTENDANCE')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 
    WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
);

-- Check results
SELECT 'Final Role Permissions:' as info;
SELECT rp.id, r.name as role_name, p.name as permission_name 
FROM role_permission rp
JOIN role r ON rp.role_id = r.id
JOIN permission p ON rp.permission_id = p.id
WHERE p.name IN ('VIEW_CHILD_ATTENDANCE', 'VIEW_DEPARTMENT_ATTENDANCE', 'VIEW_ALL_ATTENDANCE')
ORDER BY r.name, p.name;
