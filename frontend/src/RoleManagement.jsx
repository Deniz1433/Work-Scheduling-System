import React, { useState, useEffect } from 'react';

const RoleManagement = () => {
    // State'ler
    const [roles, setRoles] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [selectedRole, setSelectedRole] = useState(null);

    // Form state'leri
    const [newRole, setNewRole] = useState({ name: '', description: '' });
    const [editingRole, setEditingRole] = useState(null);

    useEffect(() => {
        fetchRoles();
    }, []);

    // Rolleri getir
    const fetchRoles = async () => {
        try {
            const response = await fetch('/api/roles');
            if (!response.ok) throw new Error('Roller yüklenemedi');
            const data = await response.json();
            setRoles(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
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

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-lg">Yükleniyor...</div>
            </div>
        );
    }

    return (
        <div className="flex flex-col items-center min-h-screen bg-gray-50 py-8">
            <h1 className="text-2xl font-bold mb-6 text-blue-800">Rol Yönetimi</h1>
            
            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 w-full max-w-6xl">
                    {error}
                </div>
            )}
            
            {success && (
                <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4 w-full max-w-6xl">
                    {success}
                </div>
            )}

            <div className="w-full max-w-6xl">
                <div className="flex gap-6">
                    {/* Sol Taraf: Rol Yönetimi */}
                    <div className="flex-1 bg-white rounded-xl shadow-lg p-6">
                        <h2 className="text-lg font-semibold mb-4 text-blue-700">Rol Yönetimi</h2>
                        
                        {/* Rol Ekleme Formu */}
                        {!editingRole ? (
                            <form onSubmit={handleAddRole} className="mb-6">
                                <div className="flex flex-col gap-4">
                                    <input
                                        type="text"
                                        placeholder="Rol Adı"
                                        value={newRole.name}
                                        onChange={e => setNewRole({...newRole, name: e.target.value})}
                                        className="border p-2 rounded focus:outline-blue-400"
                                        required
                                    />
                                    <textarea
                                        placeholder="Rol Açıklaması (opsiyonel)"
                                        value={newRole.description}
                                        onChange={e => setNewRole({...newRole, description: e.target.value})}
                                        className="border p-2 rounded focus:outline-blue-400 resize-none"
                                        rows="3"
                                    />
                                    <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700 transition-all">
                                        + Rol Ekle
                                    </button>
                                </div>
                            </form>
                        ) : (
                            <form onSubmit={handleUpdateRole} className="mb-6">
                                <div className="flex flex-col gap-4">
                                    <input
                                        type="text"
                                        placeholder="Rol Adı"
                                        value={editingRole.name}
                                        onChange={e => setEditingRole({...editingRole, name: e.target.value})}
                                        className="border p-2 rounded focus:outline-blue-400"
                                        required
                                    />
                                    <textarea
                                        placeholder="Rol Açıklaması (opsiyonel)"
                                        value={editingRole.description}
                                        onChange={e => setEditingRole({...editingRole, description: e.target.value})}
                                        className="border p-2 rounded focus:outline-blue-400 resize-none"
                                        rows="3"
                                    />
                                    <div className="flex gap-2">
                                        <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-green-700 transition-all flex-1">
                                            Güncelle
                                        </button>
                                        <button 
                                            type="button" 
                                            onClick={() => setEditingRole(null)}
                                            className="bg-gray-500 text-white px-4 py-2 rounded-lg font-semibold hover:bg-gray-600 transition-all flex-1"
                                        >
                                            İptal
                                        </button>
                                    </div>
                                </div>
                            </form>
                        )}

                        {/* Roller Listesi */}
                        <div>
                            <h3 className="text-md font-semibold mb-3 text-gray-700">Mevcut Roller</h3>
                            <div className="space-y-2 max-h-96 overflow-y-auto">
                                {roles.length === 0 ? (
                                    <p className="text-gray-500">Henüz rol eklenmemiş.</p>
                                ) : (
                                    roles.map((role) => (
                                        <div 
                                            key={role.id} 
                                            className={`flex items-center justify-between p-3 rounded-lg cursor-pointer transition-colors ${
                                                selectedRole?.id === role.id 
                                                    ? 'bg-blue-100 border border-blue-300' 
                                                    : 'bg-gray-50 hover:bg-gray-100'
                                            }`}
                                            onClick={() => setSelectedRole(role)}
                                        >
                                            <div className="flex-1">
                                                <h4 className="font-medium text-gray-900">{role.name}</h4>
                                                {role.description && (
                                                    <p className="text-sm text-gray-600 mt-1">{role.description}</p>
                                                )}
                                            </div>
                                            <div className="flex gap-2">
                                                <button
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        setEditingRole(role);
                                                    }}
                                                    className="p-2 text-blue-600 hover:bg-blue-100 rounded transition-colors"
                                                    title="Düzenle"
                                                >
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                    </svg>
                                                </button>
                                                <button
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDeleteRole(role.id);
                                                    }}
                                                    className="p-2 text-red-600 hover:bg-red-100 rounded transition-colors"
                                                    title="Sil"
                                                >
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Sağ Taraf: Yetkiler */}
                    <div className="flex-1 bg-white rounded-xl shadow-lg p-6">
                        <h2 className="text-lg font-semibold mb-4 text-blue-700">Rol Yetkileri</h2>
                        
                        {selectedRole ? (
                            <div>
                                <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                                    <h3 className="font-semibold text-blue-800">{selectedRole.name}</h3>
                                    {selectedRole.description && (
                                        <p className="text-sm text-blue-600 mt-1">{selectedRole.description}</p>
                                    )}
                                </div>
                                
                                <div className="space-y-4">
                                    <div>
                                        <h4 className="font-medium text-gray-700 mb-2">Genel Yetkiler</h4>
                                        <div className="space-y-2">
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                Admin-all
                                            </label>
                                        </div>
                                    </div>
                                    
                                    <div>
                                        <h4 className="font-medium text-gray-700 mb-2">Görüntüleme Yetkileri</h4>
                                        <div className="space-y-2">
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                view child attendance
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                view all attendance
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                view dep attendance
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                view all users
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                view all dep
                                            </label>
                                        </div>
                                    </div>
                                    
                                    <div>
                                        <h4 className="font-medium text-gray-700 mb-2">Düzenleme Yetkileri</h4>
                                        <div className="space-y-2">
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit child attendance
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit all attendance
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit dep attendance
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit user info
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit user permission
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit mazeret-sebebi
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit tatil-gunleri
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                edit dep-hiyerarşi
                                            </label>
                                        </div>
                                    </div>
                                    
                                    <div>
                                        <h4 className="font-medium text-gray-700 mb-2">Oluşturma Yetkileri</h4>
                                        <div className="space-y-2">
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                create user
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                create dep
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                create mazeret
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                create tatil
                                            </label>
                                            <label className="flex items-center">
                                                <input type="checkbox" className="mr-2" />
                                                create role
                                            </label>
                                        </div>
                                    </div>
                                    
                                    <div className="pt-4">
                                        <button className="w-full bg-green-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-green-700 transition-all">
                                            Yetkileri Kaydet
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="text-center text-gray-500 py-8">
                                <svg className="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                                <p>Yetkileri görüntülemek için sol taraftan bir rol seçin</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RoleManagement; 