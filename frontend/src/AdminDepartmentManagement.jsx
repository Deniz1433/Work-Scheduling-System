import React, { useState, useEffect } from 'react';

const DepartmentManagement = () => {
    // State'ler
    const [departments, setDepartments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    // Form state'leri
    const [newDepartment, setNewDepartment] = useState({ name: '', minDays: '' });
    const [editingDepartment, setEditingDepartment] = useState(null);

    useEffect(() => {
        fetchDepartments();
    }, []);

    // Departmanları getir
    const fetchDepartments = async () => {
        try {
            const response = await fetch('/api/departments');
            if (!response.ok) throw new Error('Departmanlar yüklenemedi');
            const data = await response.json();
            setDepartments(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // Departman ekle
    const handleAddDepartment = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        if (!newDepartment.name.trim()) {
            setError('Departman adı gereklidir');
            return;
        }

        try {
            const response = await fetch('/api/departments', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({name: newDepartment.name, minDays: parseInt(newDepartment.minDays) || 0})
            });
            console.log(newDepartment);
            if (!response.ok) throw new Error('Departman eklenemedi');
            
            setNewDepartment({ name: '', minDays: '' });
            setSuccess('Departman başarıyla eklendi!');
            fetchDepartments();
        } catch (err) {
            setError(err.message);
        }
    };

    // Departman güncelle
    const handleUpdateDepartment = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        try {
            const response = await fetch(`/api/departments/${editingDepartment.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(editingDepartment)
            });

            if (!response.ok) throw new Error('Departman güncellenemedi');
            
            setEditingDepartment(null);
            setSuccess('Departman başarıyla güncellendi!');
            fetchDepartments();
        } catch (err) {
            setError(err.message);
        }
    };

    // Departman sil
    const handleDeleteDepartment = async (id) => {
        if (!window.confirm('Bu departmanı silmek istediğinizden emin misiniz?')) return;

        try {
            const response = await fetch(`/api/departments/${id}`, { method: 'DELETE' });
            if (!response.ok) throw new Error('Departman silinemedi');
            
            setSuccess('Departman başarıyla silindi!');
            fetchDepartments();
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
            <h1 className="text-2xl font-bold mb-6 text-blue-800">Departman Yönetimi</h1>
            
            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 w-full max-w-4xl">
                    {error}
                </div>
            )}
            
            {success && (
                <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4 w-full max-w-4xl">
                    {success}
                </div>
            )}

            <div className="w-full max-w-4xl">
                <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
                    <h2 className="text-lg font-semibold mb-4 text-blue-700">Departman Yönetimi</h2>
                    
                    {/* Departman Ekleme Formu */}
                    {!editingDepartment ? (
                        <form onSubmit={handleAddDepartment} className="mb-6">
                            <div className="flex flex-col gap-4">
                                <input
                                    type="text"
                                    placeholder="Departman Adı"
                                    value={newDepartment.name}
                                    onChange={e => setNewDepartment({...newDepartment, name: e.target.value})}
                                    className="border p-2 rounded focus:outline-blue-400"
                                    required
                                />
                                <input
                                    type="number"
                                    placeholder="Departman Minimum Gün"
                                    value={newDepartment.minDays}
                                    onChange={e => setNewDepartment({...newDepartment, minDays: e.target.value})}
                                    className="border p-2 rounded focus:outline-blue-400"
                                />
                                <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700 transition-all">
                                    + Departman Ekle
                                </button>
                            </div>
                        </form>
                    ) : (
                        <form onSubmit={handleUpdateDepartment} className="mb-6">
                            <div className="flex flex-col gap-4">
                                <input
                                    type="text"
                                    placeholder="Departman Adı"
                                    value={editingDepartment.name}
                                    onChange={e => setEditingDepartment({...editingDepartment, name: e.target.value})}
                                    className="border p-2 rounded focus:outline-blue-400"
                                    required
                                />
                                <textarea
                                    placeholder="Departman minimum gün"
                                    value={editingDepartment.minDays}
                                    onChange={e => setEditingDepartment({...editingDepartment, minDays: e.target.value})}
                                    className="border p-2 rounded focus:outline-blue-400 resize-none"
                                    rows="3"
                                />
                                <div className="flex gap-2">
                                    <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-green-700 transition-all flex-1">
                                        Güncelle
                                    </button>
                                    <button 
                                        type="button" 
                                        onClick={() => setEditingDepartment(null)}
                                        className="bg-gray-500 text-white px-4 py-2 rounded-lg font-semibold hover:bg-gray-600 transition-all flex-1"
                                    >
                                        İptal
                                    </button>
                                </div>
                            </div>
                        </form>
                    )}

                    {/* Departmanlar Listesi */}
                    <div>
                        <h3 className="text-md font-semibold mb-3 text-gray-700">Mevcut Departmanlar</h3>
                        <div className="space-y-2">
                            {departments.length === 0 ? (
                                <p className="text-gray-500">Henüz departman eklenmemiş.</p>
                            ) : (
                                departments.map((department) => (
                                    <div key={department.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                                        <div className="flex-1">
                                            <h4 className="font-medium text-gray-900">{department.name}</h4>
                                            {department.minDays && (
                                                <p className="text-sm text-gray-600 mt-1">{department.minDays}</p>
                                            )}
                                        </div>
                                        <div className="flex gap-2">
                                            <button
                                                onClick={() => setEditingDepartment(department)}
                                                className="p-2 text-blue-600 hover:bg-blue-100 rounded transition-colors"
                                                title="Düzenle"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                </svg>
                                            </button>
                                            <button
                                                onClick={() => handleDeleteDepartment(department.id)}
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
            </div>
        </div>
    );
};

export default DepartmentManagement; 