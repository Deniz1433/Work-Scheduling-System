import React, { useEffect, useState } from 'react';
import Swal from 'sweetalert2';

const AdminManageUsers = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [departments, setDepartments] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filter data based on search criteria

  const handleAddDepartment = () => {
    Swal.fire({
      title: 'Departman Ekle',
      html: `
      <input type="text" id="nameAdd" class="swal2-input" placeholder="Departman Adı">
      <input type="number" id="minDaysAdd" class="swal2-input" placeholder="Minimum Gün">
      `,
      icon: 'question',
      confirmButtonText: 'Tamam',
    }).then((result) => {
      if (result.isConfirmed) {
        const newDepartment = {
          name: document.getElementById('nameAdd').value,
          minDays: document.getElementById('minDaysAdd').value,
        };
        console.log(newDepartment.name);
        if (newDepartment.name.trim() === '' || parseInt(newDepartment.minDays) < 0 || parseInt(newDepartment.minDays) > 5 || isNaN(parseInt(newDepartment.minDays))) {
          Swal.fire({
            title: 'Hata',
            text: 'Lütfen doğru bilgiler giriniz.',
            icon: 'error',
            confirmButtonText: 'Tamam'
          });
        }
        else {
          Swal.fire({
            title: 'Departman Eklendi',
            text: 'Departman başarıyla eklendi.',
            icon: 'success',
            confirmButtonText: 'Tamam',
          });
          axios.post('/api/admin/department', newDepartment)
            .then(res => {
              setDepartments([...departments, res.data]);
            })
            .catch(err => {
              console.error('Error adding department:', err);
            });
        }
      }
    });


  };
  const handleDeleteDepartment = (id) => {
    axios.delete(`/api/admin/department/${id}`)
      .then(res => {
        setDepartments(departments.filter(department => department.id !== id));
      })
      .catch(err => {
        console.error('Error deleting department:', err);
      });
  };
  const handleUpdateDepartment = (name, minDays, id) => {

    Swal.fire({
      title: 'Departman Güncelle',
      html: `
      <input type="text" id="nameUpdate" class="swal2-input" placeholder="Departman Adı" value = ${name}>
      <input type="number" id="minDaysUpdate" class="swal2-input" placeholder="Minimum Gün" value = ${minDays}>
      `,
      icon: 'question',
      confirmButtonText: 'Tamam',
    }).then((result) => {
      if (result.isConfirmed) {
        const department = {
          id: id,
          name: document.getElementById('nameUpdate').value,
          minDays: document.getElementById('minDaysUpdate').value
        };
        //console.log(department);
        axios.post(`/api/admin/department/${id}`, department)
          .then(res => {
            setDepartments(departments.map(d => d.id === id ? res.data : d));
          })
          .catch(err => {
            console.error('Error updating department:', err);
          });
      }
    });
  };
  const handleAddUser = () => {
    const departmentOptions = departments
    .map(department => `<option value="${department.id}">${department.name}</option>`)
    .join('');
    console.log(departments);
    Swal.fire({
      title: 'Kullanıcı Ekle',
      html: `
      <input type="text" id="nameAdd" class="swal2-input" placeholder="Adı">
      <input type="text" id="surnameAdd" class="swal2-input" placeholder="Soyadı">
      <input type="text" id="emailAdd" class="swal2-input" placeholder="Email">
      <input type="text" id="usernameAdd" class="swal2-input" placeholder="Kullanıcı Adı">
      <input type="text" id="passwordAdd" class="swal2-input" placeholder="Şifre">
      <select id="departmentAdd" class="swal2-input">
        ${departmentOptions}
      </select>
      <input type="text" id="positionAdd" class="swal2-input" placeholder="Pozisyon">
      `,
      icon: 'question',
      confirmButtonText: 'Tamam',
    }).then((result) => {
      if (result.isConfirmed) {
        const newUser = {
          name: document.getElementById('nameAdd').value,
          surname: document.getElementById('surnameAdd').value,
          email: document.getElementById('emailAdd').value,
          username: document.getElementById('usernameAdd').value,
          password: document.getElementById('passwordAdd').value,
          department: document.getElementById('departmentAdd').value,
          position: document.getElementById('positionAdd').value,
        };
        console.log(newUser);
      }
    });
  };
  useEffect(() => {
    axios.get('/api/admin/department')
      .then(res => {
        setDepartments(res.data);
      })
      .catch(err => {
        console.error('Error fetching users:', err);
      });
  }, []);
  //if (loading) return <div className="p-6">Loading…</div>;
  //if (error) return <div className="p-6 text-red-600">{error}</div>;
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

        {/* Departman Verileri Tablosu */}
        <div className="border border-gray-300 rounded-lg overflow-hidden">
          {/* Header */}
          <div className="bg-gray-100 border-b border-gray-300">
            <div className="grid grid-cols-4 gap-0 text-center">
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Departman ID</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Departman Adı</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Minimum Gün</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">İşlemler</div>

            </div>
          </div>

          {/* VERİ SATIRLARI */}
          <div className="bg-white">
            {departments.map((department, rowIndex) => (
              <div key={department.id} className={`grid grid-cols-4 gap-0 border-b border-gray-200 hover:bg-gray-50 ${rowIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {department?.id}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {department?.name}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {department?.minDays}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm flex gap-2 justify-center">
                  <button
                    className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
                    onClick={() => handleDeleteDepartment(department.id)}
                  >
                    Sil
                  </button>
                  <button
                    className="bg-green-500 text-white px-4 py-2 rounded-lg hover:bg-green-600"
                    onClick={() => handleUpdateDepartment(department.name, department.minDays, department.id)}
                  >
                    Güncelle
                  </button>

                </div>
              </div>
            ))}
          </div>

        </div>
        {/* Kullanıcı Verileri Tablosu */}
        <div className="border border-gray-300 rounded-lg overflow-hidden">
          {/* Header */}
          <div className="bg-gray-100 border-b border-gray-300">
            <div className="grid grid-cols-9 gap-0 text-center">
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">ID</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Adı</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Soyadı</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Email</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Kullanıcı Adı</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Şifre</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Departmanı</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">Pozisyon</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-green-600">İşlemler</div>

            </div>
          </div>
          {/* VERİ SATIRLARI */}
          <div className="bg-white">
            {departments.map((department, rowIndex) => (
              <div key={department.id} className={`grid grid-cols-4 gap-0 border-b border-gray-200 hover:bg-gray-50 ${rowIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {department?.id}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {department?.name}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {department?.minDays}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm flex gap-2 justify-center">
                  <button
                    className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
                    onClick={() => handleDeleteDepartment(department.id)}
                  >
                    Sil
                  </button>
                  <button
                    className="bg-green-500 text-white px-4 py-2 rounded-lg hover:bg-green-600"
                    onClick={() => handleUpdateDepartment(department.name, department.minDays, department.id)}
                  >
                    Güncelle
                  </button>

                </div>
              </div>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
};

export default AdminManageUsers;