import React, { useEffect, useState } from 'react';
import Swal from 'sweetalert2';
import { Pencil, Trash2, Save } from 'lucide-react';

const AdminAddUser = () => {
  const [users, setUsers] = useState([]);
  const [newUser, setNewUser] = useState({
    name: '',
    surname: '',
    email: '',
    password: '',
    position: '',
    department: '',
  });

  useEffect(() => {
    fetch('/api/admin/users')
        .then(res => res.json())
        .then(data => setUsers(data))
        .catch(() => Swal.fire('Hata', 'Kullanıcılar yüklenemedi.', 'error'));
  }, []);

  const handleChange = (e, field, index) => {
    const updated = [...users];
    updated[index][field] = e.target.value;
    setUsers(updated);
  };

  const handleUpdate = (user, index) => {
    fetch(`/api/admin/users/${user.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(user),
    })
        .then(res => {
          if (!res.ok) throw new Error();
          Swal.fire('Başarılı', 'Kullanıcı güncellendi.', 'success');
        })
        .catch(() => {
          Swal.fire('Hata', 'Güncelleme başarısız.', 'error');
        });
  };

  const handleDelete = (id) => {
    fetch(`/api/admin/users/${id}`, { method: 'DELETE' })
        .then(res => {
          if (!res.ok) throw new Error();
          setUsers(users.filter(u => u.id !== id));
          Swal.fire('Silindi', 'Kullanıcı kaldırıldı.', 'success');
        })
        .catch(() => {
          Swal.fire('Hata', 'Silme işlemi başarısız.', 'error');
        });
  };

  const handleAdd = async (e) => {
    e.preventDefault();

    const payload = {
      username: newUser.email, // Keycloak için
      email: newUser.email,
      password: newUser.password,
      name: newUser.name,
      surname: newUser.surname,
      position: newUser.position,
      department: newUser.department,
      permissionRole: "staff",      // Varsayılan
      departmentRoles: []          // Varsayılan
    };

    try {
      const res = await fetch("/api/admin/users", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        Swal.fire("Başarılı", "Kullanıcı oluşturuldu", "success");
        setUsers([...users, { ...newUser, id: users.length + 1 }]);

        // Formu sıfırla
        setNewUser({
          name: '',
          surname: '',
          email: '',
          password: '',
          position: '',
          department: '',
        });
      } else {
        const errMsg = await res.text();
        Swal.fire("Hata", `Kullanıcı oluşturulamadı: ${errMsg}`, "error");
      }
    } catch (err) {
      Swal.fire("Hata", "Sunucu hatası oluştu", "error");
    }
  };

  return (
      <div className="p-6">
        <h2 className="text-xl font-bold mb-4">Kullanıcı Ekle / Güncelle</h2>

        {/* Add Form */}
        <form onSubmit={handleAdd} className="grid grid-cols-6 gap-4 bg-gray-50 p-4 rounded-lg mb-6">
          {['name', 'surname', 'email', 'password', 'position', 'department'].map(field => (
              <input
                  key={field}
                  type={field === 'password' ? 'password' : 'text'}
                  placeholder={field.charAt(0).toUpperCase() + field.slice(1)}
                  value={newUser[field]}
                  onChange={(e) => setNewUser({ ...newUser, [field]: e.target.value })}
                  className="border px-3 py-1 rounded text-sm"
              />
          ))}
          <button type="submit" className="col-span-6 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
            Ekle
          </button>
        </form>

        {/* User Table */}
        <div className="overflow-auto">
          <table className="min-w-full bg-white divide-y divide-gray-200">
            <thead className="bg-gray-100">
            <tr>
              {['Ad', 'Soyad', 'Email', 'Pozisyon', 'Departman', 'İşlem'].map(col => (
                  <th key={col} className="px-4 py-2 text-left text-sm font-semibold">{col}</th>
              ))}
            </tr>
            </thead>
            <tbody>
            {users.map((user, index) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-4 py-2"><input className="w-full" value={user.name} onChange={(e) => handleChange(e, 'name', index)} /></td>
                  <td className="px-4 py-2"><input className="w-full" value={user.surname} onChange={(e) => handleChange(e, 'surname', index)} /></td>
                  <td className="px-4 py-2"><input className="w-full" value={user.email} onChange={(e) => handleChange(e, 'email', index)} /></td>
                  <td className="px-4 py-2"><input className="w-full" value={user.position} onChange={(e) => handleChange(e, 'position', index)} /></td>
                  <td className="px-4 py-2"><input className="w-full" value={user.department} onChange={(e) => handleChange(e, 'department', index)} /></td>
                  <td className="px-4 py-2 flex gap-2">
                    <button onClick={() => handleUpdate(user, index)} className="text-green-600 hover:underline"><Save size={16} /></button>
                    <button onClick={() => handleDelete(user.id)} className="text-red-600 hover:underline"><Trash2 size={16} /></button>
                  </td>
                </tr>
            ))}
            </tbody>
          </table>
        </div>
      </div>
  );
};

export default AdminAddUser;