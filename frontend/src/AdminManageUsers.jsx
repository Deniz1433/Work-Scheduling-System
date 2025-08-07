import React, { useEffect, useState } from 'react';
import Swal from 'sweetalert2';

const AdminManageUsers = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [editingUser, setEditingUser] = useState(null); // { userId: 1, field: 'role', value: '2' }
  const [showAttendanceModal, setShowAttendanceModal] = useState(false);
  const [selectedUserAttendance, setSelectedUserAttendance] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [isEditingAttendance, setIsEditingAttendance] = useState(false);
  const [editingAttendanceData, setEditingAttendanceData] = useState(null);
  const [newUser, setNewUser] = useState({
    firstName: '',
    lastName: '',
    email: '',
    username: '',
    password: '',
    roleId: '',
    departmentId: ''
  });

  const [filters, setFilters] = useState({
    firstName: '',
    lastName: '',
    email: '',
    department: '',
    role: ''
  });

  useEffect(() => {
    fetch('/api/admin/users')
        .then(res => res.json())
        .then(data => {
          if (Array.isArray(data)) {
            setUsers(data);
            console.log("Aktif Kullanıcı:", data)
          } else {
            console.error("Beklenmeyen veri formatı:", data);
            setUsers([]);
          }
        })
        .catch(err => {
          console.error("Kullanıcılar çekilirken hata oluştu:", err);
          setUsers([]);
        });
    fetch('/api/roles').then(res => res.json()).then(setRoles);
    fetch('/api/departments').then(res => res.json()).then(setDepartments);
  }, []);

  const handleInputChange = (e) => {
    setNewUser({ ...newUser, [e.target.name]: e.target.value });
  };

  const handleAddUser = () => {
    console.log("Gönderilecek kullanıcı verisi:", JSON.stringify(newUser, null, 2));
    fetch('/api/admin/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newUser)
    })
        .then(res => {
          if (!res.ok) throw new Error('Kullanıcı eklenemedi');
          return res.text();
        })
        .then(msg => {
          Swal.fire('Başarılı', msg, 'success');
          setNewUser({ firstName: '', lastName: '', email: '', username: '', password: '', roleId: '', departmentId: '' });
          fetch('/api/admin/users').then(res => res.json()).then(setUsers);
        })
        .catch(err => Swal.fire('Hata', err.message, 'error'));
  };

  const handleSyncUsers = () => {
    Swal.fire({
      title: 'Kullanıcıları Senkronize Et',
      text: 'Keycloak\'taki tüm kullanıcılar PostgreSQL\'e eklenecek. Devam etmek istiyor musunuz?',
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Evet, Senkronize Et',
      cancelButtonText: 'İptal'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch('/api/admin/users/sync', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        })
        .then(res => {
          if (!res.ok) throw new Error('Senkronizasyon başarısız');
          return res.text();
        })
        .then(msg => {
          Swal.fire('Başarılı', msg, 'success');
          // Kullanıcı listesini yenile
          fetch('/api/admin/users').then(res => res.json()).then(setUsers);
        })
        .catch(err => Swal.fire('Hata', err.message, 'error'));
      }
    });
  };

  const startEditing = (user, field) => {
    const value = field === 'role' ? user.roleId : user.departmentId;
    setEditingUser({ userId: user.id, field: field, value: value });
  };

  const cancelEditing = () => {
    setEditingUser(null);
  };

  const updateEditingValue = (value) => {
    setEditingUser(prev => ({ ...prev, value: value }));
  };

  const handleRoleChange = (userId, newRoleId) => {
    console.log(`Rol değiştiriliyor - User ID: ${userId}, New Role ID: ${newRoleId}`);
    
    fetch(`/api/admin/users/${userId}/role`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roleId: parseInt(newRoleId) })
    })
    .then(res => {
      if (res.ok) {
        Swal.fire('Başarılı', 'Rol başarıyla güncellendi', 'success');
        // Kullanıcı listesini güncelle
        setUsers(prevUsers => 
          prevUsers.map(user => 
            user.id === userId 
              ? { 
                  ...user, 
                  roleId: parseInt(newRoleId), 
                  role: roles.find(r => r.id === parseInt(newRoleId))?.name 
                }
              : user
          )
        );
        cancelEditing();
      } else {
        throw new Error('Rol güncellenemedi');
      }
    })
    .catch(err => {
      console.error('Rol güncelleme hatası:', err);
      Swal.fire('Hata', err.message, 'error');
    });
  };

  const handleDepartmentChange = (userId, newDeptId) => {
    console.log(`Departman değiştiriliyor - User ID: ${userId}, New Dept ID: ${newDeptId}`);
    
    fetch(`/api/admin/users/${userId}/department`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ departmentId: parseInt(newDeptId) })
    })
    .then(res => {
      if (res.ok) {
        Swal.fire('Başarılı', 'Departman başarıyla güncellendi', 'success');
        // Kullanıcı listesini güncelle
        setUsers(prevUsers => 
          prevUsers.map(user => 
            user.id === userId 
              ? { 
                  ...user, 
                  departmentId: parseInt(newDeptId), 
                  department: departments.find(d => d.id === parseInt(newDeptId))?.name 
                }
              : user
          )
        );
        cancelEditing();
      } else {
        throw new Error('Departman güncellenemedi');
      }
    })
    .catch(err => {
      console.error('Departman güncelleme hatası:', err);
      Swal.fire('Hata', err.message, 'error');
    });
  };

  const handleDeleteUser = (user) => {
    console.log('Silinecek kullanıcı:', user);
    
    Swal.fire({
      title: 'Kullanıcıyı Sil',
      text: `${user.firstName} ${user.lastName} kullanıcısını silmek istediğinizden emin misiniz?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Evet, Sil',
      cancelButtonText: 'İptal'
    }).then((result) => {
      if (result.isConfirmed) {
        // Kullanıcının keycloakId'sini kullan
        const keycloakId = user.keycloakId || user.id;
        console.log('Silme için keycloakId:', keycloakId);
        
        fetch(`/api/admin/users/${keycloakId}`, {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' }
        })
        .then(res => {
          if (res.ok) {
            return res.text();
          } else {
            throw new Error('Kullanıcı silinemedi');
          }
        })
        .then(msg => {
          Swal.fire('Başarılı', msg, 'success');
          // Kullanıcı listesini güncelle
          setUsers(prevUsers => prevUsers.filter(u => u.id !== user.id));
        })
        .catch(err => {
          console.error('Silme hatası:', err);
          Swal.fire('Hata', err.message, 'error');
        });
      }
    });
  };

  const handleAttendanceClick = (user) => {
    console.log('Attendance butonuna tıklandı, kullanıcı:', user);
    setSelectedUser(user);
    setShowAttendanceModal(true);
    
    const tryGetAttendance = (userId) => {
      return fetch(`/api/attendance/user/${userId}`)
        .then(res => res.json())
        .then(data => {
          console.log(`Attendance API response for ${userId}:`, data);
          return data;
        });
    };
    
    tryGetAttendance(user.id)
      .then(data => {
        if (data.data && data.data.attendanceRecords && data.data.attendanceRecords.length > 0) {
          formatAndSetAttendance(data);
        } else {
          setSelectedUserAttendance({ attendanceRecords: [] });
        }
      })
      .catch(err => {
        console.error('Attendance verileri alınırken hata:', err);
        setSelectedUserAttendance({ attendanceRecords: [] });
      });
  };
  
  const formatAndSetAttendance = (data) => {
    if (data.data && data.data.attendanceRecords) {
      const formattedRecords = data.data.attendanceRecords.map(record => {
        const weekStart = new Date(record.weekStart);
        const days = ['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'];
        const statusMap = {
          0: 'Veri Yok',
          1: 'Ofiste',
          2: 'Uzaktan',
          3: 'İzinli',
          4: 'Mazeretli',
          5: 'Resmi Tatil'
        };
        
        return days.map((day, index) => {
          const dayValue = [record.monday, record.tuesday, record.wednesday, record.thursday, record.friday][index];
          const date = new Date(weekStart);
          date.setDate(weekStart.getDate() + index);
          
          return {
            date: date.toLocaleDateString('tr-TR'),
            status: statusMap[dayValue] || 'Bilinmiyor',
            description: `${day} - ${statusMap[dayValue] || 'Bilinmiyor'}`,
            dayValue: dayValue,
            dayName: day,
            weekStart: record.weekStart
          };
        });
      }).flat();
      
      setSelectedUserAttendance({ attendanceRecords: formattedRecords });
      setEditingAttendanceData(data.data.attendanceRecords);
    } else {
      setSelectedUserAttendance({ attendanceRecords: [] });
      setEditingAttendanceData([]);
    }
  };

  const handleEditAttendance = () => {
    setIsEditingAttendance(true);
  };

  const handleSaveAttendance = async () => {
    try {
      const response = await fetch(`/api/attendance/user/${selectedUser.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          attendanceRecords: editingAttendanceData
        })
      });

      if (response.ok) {
        Swal.fire('Başarılı', 'Attendance kayıtları güncellendi', 'success');
        setIsEditingAttendance(false);
        handleAttendanceClick(selectedUser);
      } else {
        throw new Error('Güncelleme başarısız');
      }
    } catch (error) {
      console.error('Attendance güncelleme hatası:', error);
      Swal.fire('Hata', 'Attendance kayıtları güncellenirken hata oluştu', 'error');
    }
  };

  const handleCancelEdit = () => {
    setIsEditingAttendance(false);
  };

  const handleStatusChange = (recordIndex, newStatus) => {
    if (!editingAttendanceData) return;

    const statusMap = {
      'Veri Yok': 0,
      'Ofiste': 1,
      'Uzaktan': 2,
      'İzinli': 3,
      'Mazeretli': 4,
      'Resmi Tatil': 5
    };

    const newValue = statusMap[newStatus] || 0;
    
    const updatedData = [...editingAttendanceData];
    const record = updatedData[Math.floor(recordIndex / 5)];
    const dayIndex = recordIndex % 5;
    
    switch (dayIndex) {
      case 0: record.monday = newValue; break;
      case 1: record.tuesday = newValue; break;
      case 2: record.wednesday = newValue; break;
      case 3: record.thursday = newValue; break;
      case 4: record.friday = newValue; break;
    }
    
    setEditingAttendanceData(updatedData);
    
    const updatedRecords = [...selectedUserAttendance.attendanceRecords];
    updatedRecords[recordIndex] = {
      ...updatedRecords[recordIndex],
      status: newStatus,
      dayValue: newValue,
      description: `${updatedRecords[recordIndex].dayName} - ${newStatus}`
    };
    
    setSelectedUserAttendance({ attendanceRecords: updatedRecords });
  };

  const getRoleName = (roleId) => {
    if (!roleId) return 'Rol atanmamış';
    const role = roles.find(r => r.id.toString() === roleId.toString());
    return role ? role.name : 'Rol atanmamış';
  };

  const getDepartmentName = (departmentId) => {
    if (!departmentId) return 'Departman atanmamış';
    const dept = departments.find(d => d.id.toString() === departmentId.toString());
    return dept ? dept.name : 'Departman atanmamış';
  };

  const filteredUsers = users.filter(user => {
    return (!filters.firstName || user.firstName?.toLowerCase().includes(filters.firstName.toLowerCase())) &&
        (!filters.lastName || user.lastName?.toLowerCase().includes(filters.lastName.toLowerCase())) &&
        (!filters.email || user.email?.toLowerCase().includes(filters.email.toLowerCase())) &&
        (!filters.department || getDepartmentName(user.departmentId) === filters.department) &&
        (!filters.role || getRoleName(user.roleId) === filters.role);
  });

  const isEditing = (userId, field) => {
    return editingUser && editingUser.userId === userId && editingUser.field === field;
  };

  return (
      <div className="p-8">
        <h1 className="text-2xl font-bold mb-6">Kullanıcıları Yönet</h1>

        <div className="mb-6 border p-4 rounded shadow">
          <h2 className="text-lg font-semibold mb-4">Yeni Kullanıcı Ekle</h2>
          <div className="grid grid-cols-3 gap-4">
            <input type="text" name="firstName" placeholder="Ad" value={newUser.firstName} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="text" name="lastName" placeholder="Soyad" value={newUser.lastName} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="text" name="username" placeholder="Kullanıcı Adı" value={newUser.username} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="email" name="email" placeholder="Email" value={newUser.email} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="password" name="password" placeholder="Şifre" value={newUser.password} onChange={handleInputChange} className="border p-2 rounded" />
            <select
                name="roleId"
                value={newUser.roleId || ''}
                onChange={e => setNewUser({ ...newUser, roleId: parseInt(e.target.value) || null })}>
              <option value="">Rol Seç</option>
              {roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
            </select>
            <select
                value={newUser.departmentId || ""}
                onChange={(e) => {
                  const selectedId = parseInt(e.target.value);
                  setNewUser({
                    ...newUser,
                    departmentId: isNaN(selectedId) ? null : selectedId,
                  });
                }}
            >
              <option value="">Departman Seç</option>
              {departments.map((dept) => (
                  <option key={dept.id} value={dept.id}>
                    {dept.name}
                  </option>
              ))}
            </select>
          </div>
          <div className="flex gap-2 mt-4">
            <button onClick={handleAddUser} className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Kullanıcı Ekle
            </button>
            <button onClick={handleSyncUsers} className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
              Keycloak'tan Senkronize Et
            </button>
          </div>
        </div>
        
        <div className="grid grid-cols-5 gap-4 mb-6">
          <input type="text" placeholder="Ad" className="border p-2 rounded" value={filters.firstName} onChange={e => setFilters({ ...filters, firstName: e.target.value })} />
          <input type="text" placeholder="Soyad" className="border p-2 rounded" value={filters.lastName} onChange={e => setFilters({ ...filters, lastName: e.target.value })} />
          <input type="text" placeholder="Email" className="border p-2 rounded" value={filters.email} onChange={e => setFilters({ ...filters, email: e.target.value })} />
          <select className="border p-2 rounded" value={filters.role} onChange={e => setFilters({ ...filters, role: e.target.value })}>
            <option value="">Tüm Roller</option>
            {roles.map(r => <option key={r.id} value={r.name}>{r.name}</option>)}
          </select>
          <select className="border p-2 rounded" value={filters.department} onChange={e => setFilters({ ...filters, department: e.target.value })}>
            <option value="">Tüm Departmanlar</option>
            {departments.map(d => <option key={d.id} value={d.name}>{d.name}</option>)}
          </select>
        </div>

        <table className="w-full table-auto border border-gray-300">
          <thead>
          <tr className="bg-gray-100">
            <th className="p-2 border">Ad</th>
            <th className="p-2 border">Soyad</th>
            <th className="p-2 border">Email</th>
            <th className="p-2 border">Rol</th>
            <th className="p-2 border">Departman</th>
            <th className="p-2 border">İşlemler</th>
          </tr>
          </thead>
          <tbody>
          {filteredUsers.map(user => (
              <tr key={user.id}>
                <td className="p-2 border">{user.firstName}</td>
                <td className="p-2 border">{user.lastName}</td>
                <td className="p-2 border">{user.email}</td>
                <td className="p-2 border">
                  {isEditing(user.id, 'role') ? (
                      <div className="flex gap-2">
                        <select 
                          className="border px-2 py-1 flex-1" 
                          value={editingUser.value || ''} 
                          onChange={e => updateEditingValue(e.target.value)}
                        >
                          <option value="">Rol Seç</option>
                          {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                        </select>
                        <button 
                          onClick={() => handleRoleChange(user.id, editingUser.value)} 
                          className="bg-green-500 text-white px-2 py-1 rounded hover:bg-green-600"
                          disabled={!editingUser.value}
                        >
                          Kaydet
                        </button>
                        <button 
                          onClick={cancelEditing} 
                          className="bg-gray-500 text-white px-2 py-1 rounded hover:bg-gray-600"
                        >
                          İptal
                        </button>
                      </div>
                  ) : (
                      <div className="flex items-center gap-2">
                        <span className="flex-1 text-sm">
                          {getRoleName(user.roleId)}
                        </span>
                        <button 
                          onClick={() => startEditing(user, 'role')} 
                          className="bg-blue-500 text-white px-2 py-1 rounded hover:bg-blue-600 text-xs"
                        >
                          Değiştir
                        </button>
                      </div>
                  )}
                </td>
                <td className="p-2 border">
                  {isEditing(user.id, 'department') ? (
                      <div className="flex gap-2">
                        <select 
                          className="border px-2 py-1 flex-1" 
                          value={editingUser.value || ''} 
                          onChange={e => updateEditingValue(e.target.value)}
                        >
                          <option value="">Departman Seç</option>
                          {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                        </select>
                        <button 
                          onClick={() => handleDepartmentChange(user.id, editingUser.value)} 
                          className="bg-green-500 text-white px-2 py-1 rounded hover:bg-green-600"
                          disabled={!editingUser.value}
                        >
                          Kaydet
                        </button>
                        <button 
                          onClick={cancelEditing} 
                          className="bg-gray-500 text-white px-2 py-1 rounded hover:bg-gray-600"
                        >
                          İptal
                        </button>
                      </div>
                  ) : (
                      <div className="flex items-center gap-2">
                        <span className="flex-1 text-sm">
                          {getDepartmentName(user.departmentId)}
                        </span>
                        <button 
                          onClick={() => startEditing(user, 'department')} 
                          className="bg-purple-500 text-white px-2 py-1 rounded hover:bg-purple-600 text-xs"
                        >
                          Değiştir
                        </button>
                      </div>
                  )}
                </td>
                <td className="p-2 border">
                  <div className="flex gap-2">
                    <button 
                      onClick={() => handleAttendanceClick(user)} 
                      className="bg-orange-500 text-white px-2 py-1 rounded hover:bg-orange-600 text-xs"
                    >
                      Attendance
                    </button>
                    <button 
                      onClick={() => handleDeleteUser(user)} 
                      className="bg-red-500 text-white px-2 py-1 rounded hover:bg-red-600 text-xs"
                    >
                      Sil
                    </button>
                  </div>
                </td>
              </tr>
          ))}
          </tbody>
        </table>

        {/* Attendance Modal */}
        {showAttendanceModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded-lg shadow-lg max-w-4xl w-full max-h-[80vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold">
                  {selectedUser?.firstName} {selectedUser?.lastName} - Attendance Kayıtları
                </h2>
                <div className="flex gap-2">
                  {!isEditingAttendance ? (
                    <button 
                      onClick={handleEditAttendance}
                      className="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 text-sm"
                    >
                      Düzenle
                    </button>
                  ) : (
                    <>
                      <button 
                        onClick={handleSaveAttendance}
                        className="bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600 text-sm"
                      >
                        Kaydet
                      </button>
                      <button 
                        onClick={handleCancelEdit}
                        className="bg-gray-500 text-white px-3 py-1 rounded hover:bg-gray-600 text-sm"
                      >
                        İptal
                      </button>
                    </>
                  )}
                  <button 
                    onClick={() => setShowAttendanceModal(false)}
                    className="text-gray-500 hover:text-gray-700 text-2xl"
                  >
                    ×
                  </button>
                </div>
              </div>
              
              {selectedUserAttendance ? (
                <div>
                  <div className="mb-4">
                    <p className="text-sm text-gray-600">
                      Kullanıcı: {selectedUser?.firstName} {selectedUser?.lastName} ({selectedUser?.email})
                    </p>
                  </div>
                  
                  <table className="w-full table-auto border border-gray-300">
                    <thead>
                      <tr className="bg-gray-100">
                        <th className="p-2 border">Tarih</th>
                        <th className="p-2 border">Durum</th>
                        <th className="p-2 border">Açıklama</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedUserAttendance.attendanceRecords?.map((record, index) => (
                        <tr key={index}>
                          <td className="p-2 border">{record.date}</td>
                          <td className="p-2 border">
                            {isEditingAttendance ? (
                              <select
                                value={record.status}
                                onChange={(e) => handleStatusChange(index, e.target.value)}
                                className="border px-2 py-1 rounded text-sm w-full"
                              >
                                <option value="Veri Yok">Veri Yok</option>
                                <option value="Ofiste">Ofiste</option>
                                <option value="Uzaktan">Uzaktan</option>
                                <option value="İzinli">İzinli</option>
                                <option value="Mazeretli">Mazeretli</option>
                                <option value="Resmi Tatil">Resmi Tatil</option>
                              </select>
                            ) : (
                              <span className={`px-2 py-1 rounded text-xs ${
                                record.status === 'Ofiste' ? 'bg-green-100 text-green-800' :
                                record.status === 'Uzaktan' ? 'bg-blue-100 text-blue-800' :
                                record.status === 'İzinli' ? 'bg-yellow-100 text-yellow-800' :
                                record.status === 'Mazeretli' ? 'bg-purple-100 text-purple-800' :
                                record.status === 'Resmi Tatil' ? 'bg-red-100 text-red-800' :
                                'bg-gray-100 text-gray-800'
                              }`}>
                                {record.status}
                              </span>
                            )}
                          </td>
                          <td className="p-2 border">{record.description}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  
                  {(!selectedUserAttendance.attendanceRecords || selectedUserAttendance.attendanceRecords.length === 0) && (
                    <p className="text-center text-gray-500 py-4">
                      Bu kullanıcının henüz attendance kaydı bulunmuyor.
                    </p>
                  )}
                </div>
              ) : (
                <p className="text-center text-gray-500 py-4">
                  Attendance verileri yükleniyor...
                </p>
              )}
              
              <div className="mt-6 flex justify-end">
                <button 
                  onClick={() => setShowAttendanceModal(false)}
                  className="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600"
                >
                  Kapat
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
  );
};

export default AdminManageUsers;