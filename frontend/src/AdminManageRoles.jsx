// src/AdminManageRoles.jsx

import React, { useState, useEffect } from 'react';

const AdminManageRoles = () => {
    const [users, setUsers] = useState([]);
    const [permissionRoles, setPermissionRoles] = useState([]);
    const [departmentRoles, setDepartmentRoles] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        Promise.all([
            fetch('/api/admin/roles/permissions').then(res => res.json()),
            fetch('/api/admin/roles/departments').then(res => res.json()),
            fetch('/api/admin/users').then(res => res.json()),
        ]).then(([perms, depts, userDtos]) => {
            setPermissionRoles(perms);
            setDepartmentRoles(depts);
            setUsers(userDtos.map(u => ({
                ...u,
                selectedPermission: u.permissionRole,
                selectedDepartments: u.departmentRoles || [],
                saving: false,
                saveError: null,
            })));
            setLoading(false);
        }).catch(() => {
            setError('Failed to load data.');
            setLoading(false);
        });
    }, []);

    const toggleDepartment = (userId, role) => {
        setUsers(us =>
            us.map(u => {
                if (u.id !== userId) return u;
                const has = u.selectedDepartments.includes(role);
                const next = has
                    ? u.selectedDepartments.filter(r => r !== role)
                    : [...u.selectedDepartments, role];
                return { ...u, selectedDepartments: next, saveError: null };
            })
        );
    };

    const handlePermissionChange = (userId, newPerm) => {
        setUsers(us =>
            us.map(u =>
                u.id === userId ? { ...u, selectedPermission: newPerm, saveError: null } : u
            )
        );
    };

    const handleSave = userId => {
        setUsers(us =>
            us.map(u =>
                u.id === userId ? { ...u, saving: true, saveError: null } : u
            )
        );
        const user = users.find(u => u.id === userId);
        fetch(`/api/admin/users/${userId}/roles`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                permissionRole: user.selectedPermission,
                departmentRoles: user.selectedDepartments,
            }),
        })
            .then(res => {
                if (res.status === 403) {
                    throw new Error(
                        user.selectedPermission !== user.permissionRole
                            ? 'You cannot modify the permission level of this user.'
                            : 'You cannot modify the department roles of this user.'
                    );
                }
                if (!res.ok) throw new Error('Save failed');
            })
            .then(() => {
                setUsers(us =>
                    us.map(u =>
                        u.id === userId
                            ? {
                                ...u,
                                permissionRole: u.selectedPermission,
                                departmentRoles: u.selectedDepartments,
                            }
                            : u
                    )
                );
            })
            .catch(e => {
                setUsers(us =>
                    us.map(u =>
                        u.id === userId ? { ...u, saveError: e.message } : u
                    )
                );
            })
            .finally(() => {
                setUsers(us =>
                    us.map(u =>
                        u.id === userId ? { ...u, saving: false } : u
                    )
                );
            });
    };

    if (loading) return <div className="p-6">Loading…</div>;
    if (error) return <div className="p-6 text-red-600">{error}</div>;

    return (
        <div className="p-6">
            <h3 className="text-xl font-semibold mb-4">Rolleri Yönet</h3>
            <div className="overflow-x-auto">
                <table className="min-w-full bg-white divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                    <tr>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Permission</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Departments</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                    </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                    {users.map(user => (
                        <React.Fragment key={user.id}>
                            <tr>
                                <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-900">{user.username}</td>
                                <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-500">{user.email}</td>
                                <td className="px-4 py-2 whitespace-nowrap">
                                    <select
                                        value={user.selectedPermission}
                                        onChange={e => handlePermissionChange(user.id, e.target.value)}
                                        className="border rounded px-2 py-1 text-sm"
                                    >
                                        {permissionRoles.map(r => (
                                            <option key={r} value={r}>{r}</option>
                                        ))}
                                    </select>
                                </td>
                                <td className="px-4 py-2 whitespace-nowrap">
                                    <div className="flex flex-wrap gap-2">
                                        {departmentRoles.map(r => (
                                            <label key={r} className="inline-flex items-center bg-gray-100 px-2 py-1 rounded text-sm">
                                                <input
                                                    type="checkbox"
                                                    checked={user.selectedDepartments.includes(r)}
                                                    onChange={() => toggleDepartment(user.id, r)}
                                                    className="mr-1"
                                                />
                                                {r}
                                            </label>
                                        ))}
                                    </div>
                                </td>
                                <td className="px-4 py-2 whitespace-nowrap">
                                    <button
                                        onClick={() => handleSave(user.id)}
                                        disabled={user.saving}
                                        className={`px-3 py-1 rounded text-sm ${
                                            user.saving
                                                ? 'bg-gray-400 text-gray-200'
                                                : 'bg-blue-600 text-white hover:bg-blue-700'
                                        }`}
                                    >
                                        {user.saving ? 'Saving…' : 'Save'}
                                    </button>
                                </td>
                            </tr>
                            {user.saveError && (
                                <tr>
                                    <td colSpan={5} className="px-4 py-2">
                                        <span className="text-red-600 text-sm">{user.saveError}</span>
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default AdminManageRoles;
