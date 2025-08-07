import React, { useState, useEffect } from 'react';

const RoleManagement = () => {
    const [roles, setRoles] = useState([]);
    const [permissions, setPermissions] = useState([]);
    const [selectedRole, setSelectedRole] = useState(null);
    const [selectedPermissions, setSelectedPermissions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    const [newRole, setNewRole] = useState({ name: '', description: '' });
    const [editingRole, setEditingRole] = useState(null);

    useEffect(() => {
        fetchRoles();
        fetchPermissions();
    }, []);

    // Roller
    const fetchRoles = async () => {
        try {
            const res = await fetch('/api/roles');
            if (!res.ok) throw new Error('Roller yüklenemedi');
            const data = await res.json();
            setRoles(data);
            if (data.length > 0) {
                setSelectedRole(data[0]);
                fetchRolePermissions(data[0].id);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // Yetkiler
    const fetchPermissions = async () => {
        try {
            const res = await fetch('/api/permissions', {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
                }
            });
            if (!res.ok) throw new Error('Yetkiler yüklenemedi');
            const data = await res.json();
            setPermissions(data);
        } catch (err) {
            setError(err.message);
        }
    };

    // Seçilen rolün mevcut yetkilerini getir
    const fetchRolePermissions = async (roleId) => {
        try {
            const res = await fetch(`/api/role-permissions/${roleId}`);
            if (!res.ok) throw new Error('Rolün yetkileri getirilemedi');
            const data = await res.json();
            setSelectedPermissions(data.map(rp => rp.permission.id));
        } catch (err) {
            setError(err.message);
        }
    };

    // Checkbox değişimi
    const handlePermissionChange = (permId) => {
        setSelectedPermissions(prev =>
            prev.includes(permId)
                ? prev.filter(id => id !== permId)
                : [...prev, permId]
        );
    };

    // Yetkileri kaydet (PUT ile güncelle)
    const handleSavePermissions = async () => {
        if (!selectedRole) return;
        setError(null);
        setSuccess(null);

        try {
            const res = await fetch(`/api/role-permissions/${selectedRole.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(selectedPermissions)
            });
            console.log(selectedPermissions)
            if (!res.ok) throw new Error('Yetkiler kaydedilemedi');
            setSuccess('Yetkiler başarıyla kaydedildi!');
        } catch (err) {
            setError(err.message);
        }
    };

    // Rol ekle
    const handleAddRole = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        if (!newRole.name.trim()) {
            setError('Rol adı gereklidir');
            return;
        }

        try {
            const response = await fetch('/api/roles', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newRole)
            });

            if (!response.ok) throw new Error('Rol eklenemedi');

            setNewRole({ name: '', description: '' });
            setSuccess('Rol başarıyla eklendi!');
            fetchRoles();
        } catch (err) {
            setError(err.message);
        }
    };

    // Rol güncelle
    const handleUpdateRole = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        try {
            const response = await fetch(`/api/roles/${editingRole.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(editingRole)
            });

            if (!response.ok) throw new Error('Rol güncellenemedi');

            setEditingRole(null);
            setSuccess('Rol başarıyla güncellendi!');
            fetchRoles();
        } catch (err) {
            setError(err.message);
        }
    };

    // Rol sil
    const handleDeleteRole = async (id) => {
        if (!window.confirm('Bu rolü silmek istediğinizden emin misiniz?')) return;

        try {
            const response = await fetch(`/api/roles/${id}`, { method: 'DELETE' });
            if (!response.ok) throw new Error('Rol silinemedi');

            setSuccess('Rol başarıyla silindi!');
            fetchRoles();
        } catch (err) {
            setError(err.message);
        }
    };

    if (loading) return <div>Yükleniyor...</div>;

    return (
        <div className="flex flex-col items-center min-h-screen bg-gray-50 py-8">
            <h1 className="text-2xl font-bold mb-6 text-blue-800">Rol Yönetimi</h1>

            {error && <div className="bg-red-100 text-red-700 p-3 rounded mb-4 w-full max-w-6xl">{error}</div>}
            {success && <div className="bg-green-100 text-green-700 p-3 rounded mb-4 w-full max-w-6xl">{success}</div>}

            <div className="w-full max-w-6xl flex gap-6">
                {/* Sol taraf */}
                <div className="flex-1 bg-white p-6 rounded shadow">
                    <h2 className="text-lg font-semibold mb-4">Rol Yönetimi</h2>

                    {!editingRole ? (
                        <form onSubmit={handleAddRole} className="mb-6">
                            <input
                                type="text"
                                placeholder="Rol Adı"
                                value={newRole.name}
                                onChange={e => setNewRole({...newRole, name: e.target.value})}
                                className="border p-2 rounded mb-2 w-full"
                                required
                            />
                            <textarea
                                placeholder="Rol Açıklaması (opsiyonel)"
                                value={newRole.description}
                                onChange={e => setNewRole({...newRole, description: e.target.value})}
                                className="border p-2 rounded mb-2 w-full"
                                rows="3"
                            />
                            <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded w-full">
                                + Rol Ekle
                            </button>
                        </form>
                    ) : (
                        <form onSubmit={handleUpdateRole} className="mb-6">
                            <input
                                type="text"
                                value={editingRole.name}
                                onChange={e => setEditingRole({...editingRole, name: e.target.value})}
                                className="border p-2 rounded mb-2 w-full"
                                required
                            />
                            <textarea
                                value={editingRole.description}
                                onChange={e => setEditingRole({...editingRole, description: e.target.value})}
                                className="border p-2 rounded mb-2 w-full"
                                rows="3"
                            />
                            <div className="flex gap-2">
                                <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded flex-1">
                                    Güncelle
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setEditingRole(null)}
                                    className="bg-gray-500 text-white px-4 py-2 rounded flex-1"
                                >
                                    İptal
                                </button>
                            </div>
                        </form>
                    )}

                    <h3 className="text-md font-semibold mb-3">Mevcut Roller</h3>
                    <div className="space-y-2 max-h-96 overflow-y-auto">
                        {roles.length === 0 ? (
                            <p className="text-gray-500">Henüz rol eklenmemiş.</p>
                        ) : (
                            roles.map((role) => (
                                <div
                                    key={role.id}
                                    className={`flex items-center justify-between p-3 rounded cursor-pointer ${
                                        selectedRole?.id === role.id ? 'bg-blue-100' : 'hover:bg-gray-100'
                                    }`}
                                    onClick={() => {
                                        setSelectedRole(role);
                                        fetchRolePermissions(role.id);
                                    }}
                                >
                                    <div>
                                        <h4 className="font-medium">{role.name}</h4>
                                        {role.description && <p className="text-sm text-gray-600">{role.description}</p>}
                                    </div>
                                    <div className="flex gap-2">
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setEditingRole(role);
                                            }}
                                            className="text-blue-600"
                                        >
                                            ✏️
                                        </button>
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleDeleteRole(role.id);
                                            }}
                                            className="text-red-600"
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>

                {/* Sağ taraf */}
                <div className="flex-1 bg-white p-6 rounded shadow">
                    <h2 className="text-lg font-semibold mb-4">Rol Yetkileri</h2>

                    {selectedRole && (
                        <div className="mb-4 p-3 bg-blue-50 rounded">
                            <h3 className="font-semibold text-blue-800">{selectedRole.name}</h3>
                            {selectedRole.description && (
                                <p className="text-sm text-blue-600">{selectedRole.description}</p>
                            )}
                        </div>
                    )}

                    {permissions.length === 0 ? (
                        <p className="text-gray-500">Henüz yetki yok.</p>
                    ) : (
                        <div className="space-y-2">
                            {permissions.map((perm) => (
                                <label key={perm.id} className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={selectedPermissions.includes(perm.id)}
                                        onChange={() => handlePermissionChange(perm.id)}
                                        disabled={!selectedRole}
                                        className="mr-2"
                                    />
                                    {perm.name}
                                    {perm.description && (
                                        <span className="text-sm text-gray-500 ml-2">({perm.description})</span>
                                    )}
                                </label>
                            ))}
                        </div>
                    )}

                    <div className="pt-4">
                        <button
                            onClick={handleSavePermissions}
                            disabled={!selectedRole}
                            className={`w-full px-4 py-2 rounded font-semibold ${
                                selectedRole
                                    ? 'bg-green-600 text-white hover:bg-green-700'
                                    : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                            }`}
                        >
                            Yetkileri Kaydet
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RoleManagement;