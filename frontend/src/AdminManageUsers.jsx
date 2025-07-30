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
  const [newUser, setNewUser] = useState({
    firstName: '',
    lastName: '',
    email: '',
    username: '',
    password: ''
  });

  const [nameFilter, setNameFilter] = useState('');
  const [filterSurname, setFilterSurname] = useState('');
  const [filterEmail, setFilterEmail] = useState('');
  const [filterDept, setFilterDept] = useState('');
  const [filterRole, setFilterRole] = useState('');

  useEffect(() => {
    fetch('/api/admin/users')
        .then(res => res.json())
        .then(data => setUsers(data));

    fetch('/api/roles')
        .then(res => res.json())
        .then(data => setRoles(data));

    fetch('/api/departments')
        .then(res => res.json())
        .then(data => setDepartments(data));
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
          setNewUser({ firstName: '', lastName: '', email: '', username: '', password: '' });
        })
        .catch(err => Swal.fire('Hata', err.message, 'error'));
  };

  const handleRoleChange = (userId, newRole) => {
    setSelectedRole(newRole);
    fetch(`/api/admin/users/${userId}/role`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ role: newRole })
    }).then(() => setEditRoleUserId(null));
  };

  const handleDepartmentChange = (userId, newDept) => {
    setSelectedDepartment(newDept);
    fetch(`/api/admin/users/${userId}/department`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ department: newDept })
    }).then(() => setEditDeptUserId(null));
  };

  const filteredUsers = users.filter(user => {
    const nameMatch = nameFilter === '' || user.firstName.toLowerCase().includes(nameFilter.toLowerCase());
    const surnameMatch = filterSurname === '' || user.lastName.toLowerCase().includes(filterSurname.toLowerCase());
    const emailMatch = filterEmail === '' || user.email.toLowerCase().includes(filterEmail.toLowerCase());
    const deptMatch = filterDept === '' || user.department === filterDept;
    const roleMatch = filterRole === '' || user.role === filterRole;

    return nameMatch && surnameMatch && emailMatch && deptMatch && roleMatch;
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
          </div>
          <button onClick={handleAddUser} className="mt-4 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
            Kullanıcı Ekle
          </button>
        </div>
        <div className="grid grid-cols-5 gap-4 mb-6">
          <input
              type="text"
              placeholder="Ad"
              className="border p-2 rounded"
              value={nameFilter}
              onChange={(e) => setNameFilter(e.target.value)}
          />
          <input
              type="text"
              placeholder="Soyad"
              className="border p-2 rounded"
              value={filterSurname}
              onChange={(e) => setFilterSurname(e.target.value)}
          />
          <input
              type="text"
              placeholder="Email"
              className="border p-2 rounded"
              value={filterEmail}
              onChange={(e) => setFilterEmail(e.target.value)}
          />
          <select
              className="border p-2 rounded"
              value={filterDept}
              onChange={(e) => setFilterDept(e.target.value)}
          >
            <option value="">Tüm Departmanlar</option>
            {departments.map((d) => (
                <option key={d.id} value={d.name}>{d.name}</option>
            ))}
          </select>
          <select
              className="border p-2 rounded"
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value)}
          >
            <option value="">Tüm Roller</option>
            {roles.map((r) => (
                <option key={r.id} value={r.name}>{r.name}</option>
            ))}
          </select>
        </div>


        <table className="w-full table-auto border border-gray-300">
          <thead>
          <tr className="bg-gray-100">
            <th className="p-2 border">Ad</th>
            <th className="p-2 border">Soyad</th>
            <th className="p-2 border">Email</th>
            <th className="p-2 border">Pozisyon</th>
            <th className="p-2 border">Departman</th>
            <th className="p-2 border">İşlemler</th>
          </tr>
          </thead>
          <tbody>
          {filteredUsers.map((user) => (
              <tr key={user.id}>
                <td className="p-2 border">{user.firstName}</td>
                <td className="p-2 border">{user.lastName}</td>
                <td className="p-2 border">{user.email}</td>
                <td className="p-2 border">{user.position}</td>
                <td className="p-2 border">{user.department}</td>
                <td className="p-2 border">
                  <div className="flex gap-2 flex-wrap">
                    {editRoleUserId === user.id ? (
                        <select className="border px-2 py-1" value={selectedRole} onChange={(e) => handleRoleChange(user.id, e.target.value)}>
                          <option value="">Rol Seç</option>
                          {roles.map((r) => (
                              <option key={r} value={r}>{r}</option>
                          ))}
                        </select>
                    ) : (
                        <button onClick={() => { setEditRoleUserId(user.id); setSelectedRole(user.role || ''); }} className="bg-blue-500 text-white px-2 py-1 rounded hover:bg-blue-600">Rol Ata</button>
                    )}

                    {editDeptUserId === user.id ? (
                        <select className="border px-2 py-1" value={selectedDepartment} onChange={(e) => handleDepartmentChange(user.id, e.target.value)}>
                          <option value="">Departman Seç</option>
                          {departments.map((d) => (
                              <option key={d.id} value={d.name}>{d.name}</option>
                          ))}
                        </select>
                    ) : (
                        <button onClick={() => { setEditDeptUserId(user.id); setSelectedDepartment(user.department || ''); }} className="bg-purple-500 text-white px-2 py-1 rounded hover:bg-purple-600">Departman Ata</button>
                    )}

                    <button onClick={() => window.location.href = `/attendance/edit/${user.id}`} className="bg-orange-500 text-white px-2 py-1 rounded hover:bg-orange-600">Attendance</button>
                  </div>
                </td>
              </tr>
          ))}
          </tbody>
        </table>
      </div>
  );
};

export default AdminManageUsers;