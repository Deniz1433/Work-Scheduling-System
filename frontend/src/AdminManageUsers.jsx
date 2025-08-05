import React, { useEffect, useState } from 'react';
import Swal from 'sweetalert2';

const AdminManageUsers = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [editRoleUserId, setEditRoleUserId] = useState(null);
  const [editDeptUserId, setEditDeptUserId] = useState(null);
  const [selectedRole, setSelectedRole] = useState('');
  const [selectedDepartment, setSelectedDepartment] = useState('');
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
          } else {
            console.error("Beklenmeyen veri formatı:", data);
            setUsers([]); // Hatalı veri gelirse boş dizi ata
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

  const handleRoleChange = (userId, newRoleId) => {
    fetch(`/api/admin/users/${userId}/role`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roleId: newRoleId })
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
                  roleId: newRoleId, 
                  role: roles.find(r => r.id.toString() === newRoleId)?.name 
                }
              : user
          )
        );
        setEditRoleUserId(null);
      } else {
        throw new Error('Rol güncellenemedi');
      }
    })
    .catch(err => {
      Swal.fire('Hata', err.message, 'error');
    });
  };

  const handleDepartmentChange = (userId, newDeptId) => {
    fetch(`/api/admin/users/${userId}/department`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ departmentId: newDeptId })
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
                  departmentId: newDeptId, 
                  department: departments.find(d => d.id.toString() === newDeptId)?.name 
                }
              : user
          )
        );
        setEditDeptUserId(null);
      } else {
        throw new Error('Departman güncellenemedi');
      }
    })
    .catch(err => {
      Swal.fire('Hata', err.message, 'error');
    });
  };

  const handleDeleteUser = (user) => {
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
        fetch(`/api/admin/users/${user.id}`, {
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
          Swal.fire('Hata', err.message, 'error');
        });
      }
    });
  };

  const handleAttendanceClick = (user) => {
    console.log('Attendance butonuna tıklandı, kullanıcı:', user);
    console.log('Kullanıcı ID:', user.id);
    setSelectedUser(user);
    setShowAttendanceModal(true);
    
    // Önce Keycloak ID ile dene, sonra Long ID ile dene
    const tryGetAttendance = (userId) => {
      return fetch(`/api/attendance/user/${userId}`)
        .then(res => res.json())
        .then(data => {
          console.log(`Attendance API response for ${userId}:`, data);
          return data;
        });
    };
    
    // Önce Keycloak ID ile dene
    tryGetAttendance(user.id)
      .then(data => {
        if (data.data && data.data.attendanceRecords && data.data.attendanceRecords.length > 0) {
          // Keycloak ID ile veri bulundu
          formatAndSetAttendance(data);
        } else {
          // Keycloak ID ile veri bulunamadı, Long ID ile dene
          console.log('Keycloak ID ile veri bulunamadı, Long ID ile deneniyor...');
          // Long ID'yi tahmin et (genellikle 1, 2, 3...)
          const possibleLongIds = ['1', '2', '3', '4', '5'];
          
          const tryLongIds = async () => {
            for (const longId of possibleLongIds) {
              try {
                const longData = await tryGetAttendance(longId);
                if (longData.data && longData.data.attendanceRecords && longData.data.attendanceRecords.length > 0) {
                  console.log(`Long ID ${longId} ile veri bulundu`);
                  formatAndSetAttendance(longData);
                  return;
                }
              } catch (err) {
                console.log(`Long ID ${longId} ile hata:`, err);
              }
            }
            // Hiçbir ID ile veri bulunamadı
            setSelectedUserAttendance({ attendanceRecords: [] });
          };
          
          tryLongIds();
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
      // Düzenleme için ham veriyi de sakla
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
      // Düzenlenen verileri backend'e gönder
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
        // Verileri yeniden yükle
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
    
    // Ham veriyi güncelle
    const updatedData = [...editingAttendanceData];
    const record = updatedData[Math.floor(recordIndex / 5)]; // Hangi hafta
    const dayIndex = recordIndex % 5; // Hangi gün (0-4)
    
    switch (dayIndex) {
      case 0: record.monday = newValue; break;
      case 1: record.tuesday = newValue; break;
      case 2: record.wednesday = newValue; break;
      case 3: record.thursday = newValue; break;
      case 4: record.friday = newValue; break;
    }
    
    setEditingAttendanceData(updatedData);
    
    // Görüntülenen veriyi de güncelle
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
            <select name="roleId" value={newUser.roleId || ''} onChange={handleInputChange} className="border p-2 rounded">
              <option value="">Rol Seç</option>
              {roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
            </select>
            <select name="departmentId" value={newUser.departmentId || ''} onChange={handleInputChange} className="border p-2 rounded">
              <option value="">Departman Seç</option>
              {departments.map(dept => <option key={dept.id} value={dept.id}>{dept.name}</option>)}
            </select>
          </div>
          <button onClick={handleAddUser} className="mt-4 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
            Kullanıcı Ekle
          </button>
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
                  {editRoleUserId === user.id ? (
                      <div className="flex gap-2">
                        <select 
                          className="border px-2 py-1 flex-1" 
                          value={selectedRole} 
                          onChange={e => setSelectedRole(e.target.value)}
                        >
                          <option value="">Rol Seç</option>
                          {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                        </select>
                        <button 
                          onClick={() => handleRoleChange(user.id, selectedRole)} 
                          className="bg-green-500 text-white px-2 py-1 rounded hover:bg-green-600"
                          disabled={!selectedRole}
                        >
                          Kaydet
                        </button>
                        <button 
                          onClick={() => setEditRoleUserId(null)} 
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
                          onClick={() => { 
                            setEditRoleUserId(user.id); 
                            setSelectedRole(user.roleId || ''); 
                          }} 
                          className="bg-blue-500 text-white px-2 py-1 rounded hover:bg-blue-600 text-xs"
                        >
                          Değiştir
                        </button>
                      </div>
                  )}
                </td>
                <td className="p-2 border">
                  {editDeptUserId === user.id ? (
                      <div className="flex gap-2">
                        <select 
                          className="border px-2 py-1 flex-1" 
                          value={selectedDepartment} 
                          onChange={e => setSelectedDepartment(e.target.value)}
                        >
                          <option value="">Departman Seç</option>
                          {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                        </select>
                        <button 
                          onClick={() => handleDepartmentChange(user.id, selectedDepartment)} 
                          className="bg-green-500 text-white px-2 py-1 rounded hover:bg-green-600"
                          disabled={!selectedDepartment}
                        >
                          Kaydet
                        </button>
                        <button 
                          onClick={() => setEditDeptUserId(null)} 
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
                          onClick={() => { 
                            setEditDeptUserId(user.id); 
                            setSelectedDepartment(user.departmentId || ''); 
                          }} 
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
