import React, { useEffect, useState } from 'react';
import Swal from 'sweetalert2';
import axios from 'axios';

const AdminManageUsers = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editRoleUserId, setEditRoleUserId] = useState(null);
  const [editDeptUserId, setEditDeptUserId] = useState(null);
  const [selectedRole, setSelectedRole] = useState('');
  const [selectedDepartment, setSelectedDepartment] = useState('');
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

  // Handle form inputs for newUser
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewUser(prev => ({ ...prev, [name]: value }));
  };

  // Department CRUD handlers
  const handleAddDepartment = () => {
    Swal.fire({
      title: 'Departman Ekle',
      html: `
        <input type=\"text\" id=\"nameAdd\" class=\"swal2-input\" placeholder=\"Departman Adı\">        
        <input type=\"number\" id=\"minDaysAdd\" class=\"swal2-input\" placeholder=\"Minimum Gün\">      
      `,
      icon: 'question',
      confirmButtonText: 'Tamam',
    }).then((result) => {
      if (result.isConfirmed) {
        const newDepartment = {
          name: document.getElementById('nameAdd').value,
          minDays: document.getElementById('minDaysAdd').value,
        };
        if (
            !newDepartment.name.trim() ||
            parseInt(newDepartment.minDays) < 0 ||
            parseInt(newDepartment.minDays) > 5 ||
            isNaN(parseInt(newDepartment.minDays))
        ) {
          Swal.fire('Hata', 'Lütfen doğru bilgiler giriniz.', 'error');
        } else {
          axios.post('/api/admin/department', newDepartment)
              .then(res => {
                setDepartments(prev => [...prev, res.data]);
                Swal.fire('Departman Eklendi', 'Departman başarıyla eklendi.', 'success');
              })
              .catch(err => console.error('Error adding department:', err));
        }
      }
    });
  };

  const handleDeleteDepartment = (id) => {
    axios.delete(`/api/admin/department/${id}`)
        .then(() => setDepartments(prev => prev.filter(d => d.id !== id)))
        .catch(err => console.error('Error deleting department:', err));
  };

  const handleUpdateDepartment = (name, minDays, id) => {
    Swal.fire({
      title: 'Departman Güncelle',
      html: `
        <input type=\"text\" id=\"nameUpdate\" class=\"swal2-input\" placeholder=\"Departman Adı\" value=\"${name}\">        
        <input type=\"number\" id=\"minDaysUpdate\" class=\"swal2-input\" placeholder=\"Minimum Gün\" value=\"${minDays}\">      
      `,
      icon: 'question',
      confirmButtonText: 'Tamam',
    }).then((result) => {
      if (result.isConfirmed) {
        const updated = {
          id,
          name: document.getElementById('nameUpdate').value,
          minDays: document.getElementById('minDaysUpdate').value,
        };
        axios.post(`/api/admin/department/${id}`, updated)
            .then(res => setDepartments(prev => prev.map(d => d.id === id ? res.data : d)))
            .catch(err => console.error('Error updating department:', err));
      }
    });
  };

  // User CRUD & assignment handlers
  const handleAddUser = () => {
    axios.post('/api/admin/users', newUser)
        .then(() => {
          Swal.fire('Başarılı', 'Kullanıcı başarıyla eklendi.', 'success');
          setNewUser({ firstName: '', lastName: '', email: '', username: '', password: '', roleId: '', departmentId: '' });
          return axios.get('/api/admin/users');
        })
        .then(res => setUsers(res.data))
        .catch(err => Swal.fire('Hata', err.message || 'Kullanıcı eklenemedi', 'error'));
  };

  const handleRoleChange = (userId, newRoleId) => {
    axios.put(`/api/admin/users/${userId}/role`, { roleId: newRoleId })
        .then(() => setEditRoleUserId(null));
  };

  const handleDepartmentChange = (userId, newDeptId) => {
    axios.put(`/api/admin/users/${userId}/department`, { departmentId: newDeptId })
        .then(() => setEditDeptUserId(null));
  };

  // Filtered users
  const filteredUsers = users.filter(user => (
      (!filters.firstName || user.firstName.toLowerCase().includes(filters.firstName.toLowerCase())) &&
      (!filters.lastName || user.lastName.toLowerCase().includes(filters.lastName.toLowerCase())) &&
      (!filters.email || user.email.toLowerCase().includes(filters.email.toLowerCase())) &&
      (!filters.department || user.department === filters.department) &&
      (!filters.role || user.role === filters.role)
  ));

  // Initial data fetch
  useEffect(() => {
    axios.get('/api/admin/department')
        .then(res => { setDepartments(res.data); setLoading(false); })
        .catch(err => { console.error(err); setError('Departman verileri alınamadı.'); setLoading(false); });
    axios.get('/api/admin/users')
        .then(res => setUsers(res.data))
        .catch(err => console.error(err));
    axios.get('/api/roles')
        .then(res => setRoles(res.data))
        .catch(err => console.error(err));
  }, []);

  if (loading) return <div className="p-6">Loading…</div>;
  if (error) return <div className="p-6 text-red-600">{error}</div>;

  return (
      <div className="p-8">
        <h1 className="text-2xl font-bold mb-6">Kullanıcıları Yönet</h1>

        {/* Yeni Kullanıcı Ekle */}
        <div className="mb-6 border p-4 rounded shadow">
          <h2 className="text-lg font-semibold mb-4">Yeni Kullanıcı Ekle</h2>
          <div className="grid grid-cols-3 gap-4">
            <input type="text" name="firstName" placeholder="Ad" value={newUser.firstName} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="text" name="lastName" placeholder="Soyad" value={newUser.lastName} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="text" name="username" placeholder="Kullanıcı Adı" value={newUser.username} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="email" name="email" placeholder="Email" value={newUser.email} onChange={handleInputChange} className="border p-2 rounded" />
            <input type="password" name="password" placeholder="Şifre" value={newUser.password} onChange={handleInputChange} className="border p-2 rounded" />
            <select name="roleId" value={newUser.roleId} onChange={handleInputChange} className="border p-2 rounded">
              <option value="">Rol Seç</option>
              {roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
            </select>
            <select name="departmentId" value={newUser.departmentId} onChange={handleInputChange} className="border p-2 rounded">
              <option value="">Departman Seç</option>
              {departments.map(dept => <option key={dept.id} value={dept.id}>{dept.name}</option>)}
            </select>
          </div>
          <button onClick={handleAddUser} className="mt-4 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">Kullanıcı Ekle</button>
        </div>

        {/* Departman Yönetimi */}
        <div className="mb-6 flex gap-2">
          <button onClick={handleAddDepartment} className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Departman Ekle</button>
        </div>
        <div className="border border-gray-300 rounded-lg overflow-hidden mb-6">
          <div className="bg-gray-100 border-b border-gray-300">
            <div className="grid grid-cols-4 text-center">
              <div className="p-3 font-semibold bg-green-600 text-white">ID</div>
              <div className="p-3 font-semibold bg-green-600 text-white">Adı</div>
              <div className="p-3 font-semibold bg-green-600 text-white">Min Gün</div>
              <div className="p-3 font-semibold bg-green-600 text-white">İşlemler</div>
            </div>
          </div>
          <div>
            {departments.map((dept, idx) => (
                <div key={dept.id} className={`grid grid-cols-4 border-b ${idx % 2 ? 'bg-gray-50' : 'bg-white'}`}>
                  <div className="p-3">{dept.id}</div>
                  <div className="p-3">{dept.name}</div>
                  <div className="p-3">{dept.minDays}</div>
                  <div className="p-3 flex gap-2">
                    <button onClick={() => handleDeleteDepartment(dept.id)} className="bg-red-500 text-white px-2 py-1 rounded hover:bg-red-600">Sil</button>
                    <button onClick={() => handleUpdateDepartment(dept.name, dept.minDays, dept.id)} className="bg-green-500 text-white px-2 py-1 rounded hover:bg-green-600">Güncelle</button>
                  </div>
                </div>
            ))}
          </div>
        </div>

        {/* Filtreleme */}
        <div className="grid grid-cols-5 gap-4 mb-6">
          <input type="text" placeholder="Ad" value={filters.firstName} onChange={e => setFilters(prev => ({ ...prev, firstName: e.target.value }))} className="border p-2 rounded" />
          <input type="text" placeholder="Soyad" value={filters.lastName} onChange={e => setFilters(prev => ({ ...prev, lastName: e.target.value }))} className="border p-2 rounded" />
          <input type="text" placeholder="Email" value={filters.email} onChange={e => setFilters(prev => ({ ...prev, email: e.target.value }))} className="border p-2 rounded" />
          <select value={filters.role} onChange={e => setFilters(prev => ({ ...prev, role: e.target.value }))} className="border p-2 rounded">
            <option value="">Tüm Roller</option>
            {roles.map(r => <option key={r.id} value={r.name}>{r.name}</option>)}
          </select>
          <select value={filters.department} onChange={e => setFilters(prev => ({ ...prev, department: e.target.value }))} className="border p-2 rounded">
            <option value="">Tüm Departmanlar</option>
            {departments.map(d => <option key={d.id} value={d.name}>{d.name}</option>)}
          </select>
        </div>

        {/* Kullanıcı Tablosu */}
        <table className="w-full border-collapse border border-gray-300">
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
              <tr key={user.id} className="hover:bg-gray-50">
                <td className="p-2 border">{user.firstName}</td>
                <td className="p-edd2 border">{user.lastName}</td>
                <td className="p-2 border">{user.email}</td>
                <td className="p-2 border">
                  {editRoleUserId === user.id ? (
                      <select value={selectedRole} onChange={e => handleRoleChange(user.id, e.target.value)} className="border p-1 rounded">
                        <option value="">Rol Seç</option>
                        {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                      </select>
                  ) : (
                      <button onClick={() => { setEditRoleUserId(user.id); setSelectedRole(user.roleId || ''); }} className="bg-blue-500 text-white px-2 py-1 rounded hover:bg-blue-600">Rol Ata</button>
                  )}
                </td>
                <td className="p-2 border">
                  {editDeptUserId === user.id ? (
                      <select value={selectedDepartment} onChange={e => handleDepartmentChange(user.id, e.target.value)} className="border p-1 rounded">
                        <option value="">Departman Seç</option>
                        {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                      </select>
                  ) : (
                      <button onClick={() => { setEditDeptUserId(user.id); setSelectedDepartment(user.departmentId || ''); }} className="bg-purple-500 text-white px-2 py-1 rounded hover:bg-purple-600">Departman Ata</button>
                  )}
                </td>
                <td className="p-2 border">
                  <button onClick={() => window.location.href = `/attendance/edit/${user.id}`} className="bg-orange-500 text-white px-2 py-1 rounded hover:bg-orange-600">Attendance</button>
                </td>
              </tr>
          ))}
          </tbody>
        </table>
      </div>
  );
};

export default AdminManageUsers;