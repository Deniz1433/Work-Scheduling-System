// src/frontend/src/AdminAddUser.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Search, OctagonMinus } from 'lucide-react';
import Swal from 'sweetalert2';

export default function AdminAddUser() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);

  // Search filters
  const [nameSearch, setNameSearch] = useState('');
  const [surnameSearch, setSurnameSearch] = useState('');
  const [emailSearch, setEmailSearch] = useState('');

  // Role selector state
  const [roleSearch, setRoleSearch] = useState('');
  const [selectedRoles, setSelectedRoles] = useState([]);

  // Add form state
  const [addName, setAddName] = useState('');
  const [addSurname, setAddSurname] = useState('');
  const [addEmail, setAddEmail] = useState('');
  const [addPassword, setAddPassword] = useState('');
  const [addDepartment, setAddDepartment] = useState('');

  // Fetch users when filters change
  useEffect(() => {
    axios
        .get('/api/users', {
          params: { name: nameSearch, surname: surnameSearch, email: emailSearch },
          withCredentials: true,
        })
        .then(res => setUsers(res.data))
        .catch(console.error);
  }, [nameSearch, surnameSearch, emailSearch]);

  // Fetch roles once
  useEffect(() => {
    axios
        .get('/api/roles', { withCredentials: true })
        .then(res => setRoles(res.data))
        .catch(console.error);
  }, []);

  const reloadUsers = () => {
    axios
        .get('/api/users', { withCredentials: true })
        .then(res => setUsers(res.data))
        .catch(console.error);
  };

  const toggleRole = role => {
    setSelectedRoles(prev =>
        prev.includes(role)
            ? prev.filter(r => r !== role)
            : [...prev, role]
    );
  };

  const handleAdd = e => {
    e.preventDefault();
    axios
        .post(
            '/api/users',
            {
              name: addName,
              surname: addSurname,
              email: addEmail,
              password: addPassword,
              roles: selectedRoles,
              department: addDepartment,
            },
            { withCredentials: true }
        )
        .then(() => {
          setAddName('');
          setAddSurname('');
          setAddEmail('');
          setAddPassword('');
          setSelectedRoles([]);
          setAddDepartment('');
          reloadUsers();
          Swal.fire('Başarılı', 'Kullanıcı eklendi', 'success');
        })
        .catch(() => Swal.fire('Hata', 'Ekleme başarısız', 'error'));
  };

  const handleRemove = id => {
    Swal.fire({
      title: 'Silinsin mi?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Evet',
      cancelButtonText: 'Hayır',
    }).then(({ isConfirmed }) => {
      if (!isConfirmed) return;
      axios
          .delete(`/api/users/${id}`, { withCredentials: true })
          .then(() => {
            reloadUsers();
            Swal.fire('Silindi', 'Kullanıcı silindi', 'success');
          })
          .catch(() => Swal.fire('Hata', 'Silme başarısız', 'error'));
    });
  };

  return (
      <div className="p-6 bg-white">
        <h3 className="text-lg font-semibold mb-4">Kullanıcı Yönetimi</h3>

        {/* Search filters */}
        <div className="flex flex-wrap gap-4 mb-6">
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <input
                type="text"
                placeholder="İsim"
                value={nameSearch}
                onChange={e => setNameSearch(e.target.value)}
                className="border px-2 py-1 text-sm"
            />
          </div>
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <input
                type="text"
                placeholder="Soyisim"
                value={surnameSearch}
                onChange={e => setSurnameSearch(e.target.value)}
                className="border px-2 py-1 text-sm"
            />
          </div>
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <input
                type="text"
                placeholder="Email"
                value={emailSearch}
                onChange={e => setEmailSearch(e.target.value)}
                className="border px-2 py-1 text-sm"
            />
          </div>
        </div>

        {/* Add user form */}
        <form onSubmit={handleAdd} className="flex flex-wrap gap-4 mb-6 bg-gray-50 p-4 rounded-lg">
          <input
              type="text"
              placeholder="Ad"
              value={addName}
              onChange={e => setAddName(e.target.value)}
              className="border px-2 py-1 text-sm w-32"
              required
          />
          <input
              type="text"
              placeholder="Soyad"
              value={addSurname}
              onChange={e => setAddSurname(e.target.value)}
              className="border px-2 py-1 text-sm w-32"
              required
          />
          <input
              type="email"
              placeholder="Email"
              value={addEmail}
              onChange={e => setAddEmail(e.target.value)}
              className="border px-2 py-1 text-sm w-40"
              required
          />
          <input
              type="password"
              placeholder="Şifre"
              value={addPassword}
              onChange={e => setAddPassword(e.target.value)}
              className="border px-2 py-1 text-sm w-32"
              required
          />
          {/* Roles multi-select dropdown */}
          <div className="relative w-64">
            <input
                type="text"
                placeholder="Roller ara..."
                value={roleSearch}
                onChange={e => setRoleSearch(e.target.value)}
                className="border px-2 py-1 text-sm w-full"
            />
            <div className="absolute bg-white border mt-1 max-h-40 overflow-y-auto w-full z-10">
              {roles
                  .filter(r => r.toLowerCase().includes(roleSearch.toLowerCase()))
                  .map(r => (
                      <label key={r} className="flex items-center px-2 py-1 hover:bg-gray-100">
                        <input
                            type="checkbox"
                            checked={selectedRoles.includes(r)}
                            onChange={() => toggleRole(r)}
                            className="mr-2"
                        />
                        <span className="text-sm">{r}</span>
                      </label>
                  ))}
            </div>
          </div>
          <input
              type="text"
              placeholder="Departman"
              value={addDepartment}
              onChange={e => setAddDepartment(e.target.value)}
              className="border px-2 py-1 text-sm w-32"
          />
          <button
              type="submit"
              className="bg-blue-600 text-white px-4 py-1 rounded-lg hover:bg-blue-700"
          >
            Ekle
          </button>
        </form>

        {/* Users table */}
        <div className="border rounded-lg overflow-hidden">
          <div className="grid grid-cols-7 bg-gray-100 text-sm font-semibold text-center">
            <div className="p-2 border-r">ID</div>
            <div className="p-2 border-r">Ad</div>
            <div className="p-2 border-r">Soyad</div>
            <div className="p-2 border-r">E-posta</div>
            <div className="p-2 border-r">Rol</div>
            <div className="p-2 border-r">Departman</div>
            <div className="p-2">İşlemler</div>
          </div>
          {users.map((u, i) => (
              <div
                  key={u.id} className={`grid grid-cols-7 text-center text-sm border-b ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}>
                <div className="p-2 border-r">{u.id}</div>
                <div className="p-2 border-r">{u.name}</div>
                <div className="p-2 border-r">{u.surname}</div>
                <div className="p-2 border-r">{u.email}</div>
                <div className="p-2 border-r">{u.roles?.join(', ')}</div>
                <div className="p-2 border-r">{u.department}</div>
                <div className="p-2 flex justify-center">
                  <button
                      onClick={() => handleRemove(u.id)}
                      className="text-red-600 hover:text-red-800 flex items-center"
                      title="Kaldır"
                  >
                    <OctagonMinus className="w-4 h-4 mr-1" />
                    Kaldır
                  </button>
                </div>
              </div>
          ))}
        </div>
      </div>
  );
}
