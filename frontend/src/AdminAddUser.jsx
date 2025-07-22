import React, { useState } from 'react';
import { Search, OctagonMinus } from 'lucide-react';
import Swal from 'sweetalert2';

const AdminAddUser = () => {

  const [nameSearch, setNameSearch] = useState('');
  const [surnameSearch, setSurnameSearch] = useState('');
  const [emailSearch, setEmailSearch] = useState('');
  const [passwordSearch, setPasswordSearch] = useState('');
  const [positionSearch, setPositionSearch] = useState('');
  const [departmentSearch, setDepartmentSearch] = useState('');
  
  // Ekleme formu state'leri
  const [addName, setAddName] = useState('');
  const [addSurname, setAddSurname] = useState('');
  const [addEmail, setAddEmail] = useState('');
  const [addPassword, setAddPassword] = useState('');
  const [addPosition, setAddPosition] = useState('');
  const [addDepartment, setAddDepartment] = useState('');
  
  // Sample data - replace with your actual data source
  const [employeeData, setEmployeeData] = useState([
    {
      id: 1,
      name: 'AHMET',
      surname: 'GÜREL',
      email: 'ahmet@email.com',
      password: 'password1',
      department: 'Java',
      position: 'Çalışan'
      },
      {
      id: 2,
      name: 'AYŞEGÜL',
      email: 'aysegul@email.com',
      password: 'password2',
      role:'Takım kaptanı',
      department: 'Mobilite',
      position: 'Takım kaptanı'
      },
      {
      id: 3,
      name: 'EMRE ÇAKIR',
      surname: 'YANCI',
      email: 'emre@email.com',
      password: 'password3',
      department: 'Java',
      position:'Çalışan'
      },
      {
      id: 4,
      name: 'CEYHUN',
      surname: 'ARABA',
      email: 'ceyhun@email.com',
      password: 'password4',
      department: '',
      position: 'Genel Müdür'
      },
      {
      id: 5,
      name: 'İnsan',
      surname: 'Kaynak',
      email: 'ik@email.com',
      password: 'password5',
      role:'İnsan Kaynakları',
      department: '',
      position: 'İnsan Kaynakları'
      }
  ]);
  // Yeni kullanıcı ekleme
  const handleAdd = (e) => {
    e.preventDefault();
    const maxId = employeeData.reduce((max, employee) => Math.max(max, employee.id), 0);
    const newEmployee = {
      id: maxId + 1,
      name: addName,
      surname: addSurname,
      email: addEmail,
      password: addPassword,
      position: addPosition,
      department: addDepartment,
    };
    setEmployeeData([...employeeData, newEmployee]);
  };

  // Kullanıcıyı silme
  const handleRemove = (employeeId) => {
    setEmployeeData(employeeData.filter((employee) => employee.id !== employeeId));
    console.log(employeeId + "removed.");
  };
  // Filter data based on search criteria
  const filteredData = employeeData?.filter(employee => {
    const nameMatch = !nameSearch || (employee.name?.toLowerCase().includes(nameSearch.toLowerCase()) || !employee.name);
    const surnameMatch = !surnameSearch || (employee.surname && employee.surname.toLowerCase().includes(surnameSearch.toLowerCase()));
    const emailMatch = !emailSearch || (employee.email?.toLowerCase().includes(emailSearch.toLowerCase()) || !employee.email);
    const passwordMatch = !passwordSearch || (employee.password?.toLowerCase().includes(passwordSearch.toLowerCase()) || !employee.password);
    const positionMatch = !positionSearch || (employee.position?.toLowerCase().includes(positionSearch.toLowerCase()) || !employee.position);
    const departmentMatch = !departmentSearch || (employee.department?.toLowerCase().includes(departmentSearch.toLowerCase()) || !employee.department);
    return nameMatch && surnameMatch && emailMatch && passwordMatch && positionMatch && departmentMatch;
  });

  return (
    <div className="flex-1 p-6 bg-white">
      <div className="mb-6">
        <div className="flex item-center justify-between mb-6"></div>
        {/* Sorgu Kısmı */}
        <div className="flex items-center gap-4 mb-6 flex-wrap">
          {/* İsim Sorgusu */}
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <span className="text-sm font-medium">İsim Sorgusu</span>
            <input
              type="text"
              value={nameSearch}
              onChange={(e) => setNameSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          {/* Soyisim Sorgusu */}
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <span className="text-sm font-medium">Soyisim Sorgusu</span>
            <input
              type="text"
              value={surnameSearch}
              onChange={(e) => setSurnameSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          {/* Email Sorgusu */}
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <span className="text-sm font-medium">Email Sorgusu</span>
            <input
              type="text"
              value={emailSearch}
              onChange={(e) => setEmailSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          {/* Parola Sorgusu */}
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <Search className="w-4 h-4 text-gray-500" />
            <span className="text-sm font-medium">Parola Sorgusu</span>
            <input
              type="password"
              value={passwordSearch}
              onChange={(e) => setPasswordSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          {/* Pozisyon Sorgusu */}
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <span className="text-sm font-medium">Pozisyon Sorgusu</span>
            <select
              value={positionSearch}
              onChange={(e) => setPositionSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-40 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">Tümü</option>
              <option value="Çalışan">Çalışan</option>
              <option value="İnsan Kaynakları">İnsan Kaynakları</option>
              <option value="Admin">Admin</option>
              <option value="Takım Kaptanı">Takım Kaptanı</option>
              <option value="Genel Müdür">Genel Müdür</option>
            </select>
          </div>
          {/* Departman Sorgusu */}
          <div className="flex items-center gap-2 bg-gray-50 p-2 rounded-lg">
            <span className="text-sm font-medium">Departman Sorgusu</span>
            <select
              value={departmentSearch}
              onChange={(e) => setDepartmentSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-40 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">Tümü</option>
              <option value="Java">Java</option>
              <option value="Mobilite">Mobilite</option>
            </select>
          </div>
        </div>
        {/* Veri Tablosu */}
        <div className="border border-gray-300 rounded-lg overflow-hidden">
          {/* Header */}
          <div className="bg-gray-100 border-b border-gray-300">
            <div className="grid grid-cols-8 gap-0 text-center">
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">ID</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Ad</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Soyad</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">E-posta</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Şifre</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Pozisyon</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Departman</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200 ">İşlemler</div>
            </div>
          </div>
        {/* Ekleme Formu */}
          <form onSubmit={handleAdd} className="flex flex-col gap-4 mb-6 bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium">Yeni Kullanıcı Ekle</span>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={addName}
                onChange={(e) => setAddName(e.target.value)}
                placeholder="Ad"
                className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <input
                type="text"
                value={addSurname}
                onChange={(e) => setAddSurname(e.target.value)}
                placeholder="Soyad"
                className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <input
                type="text"
                value={addEmail}
                onChange={(e) => setAddEmail(e.target.value)}
                placeholder="Email"
                className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <input
                type="password"
                value={addPassword}
                onChange={(e) => setAddPassword(e.target.value)}
                placeholder="Şifre"
                className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <input
                type="text"
                value={addPosition}
                onChange={(e) => setAddPosition(e.target.value)}
                placeholder="Pozisyon"
                className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <input
                type="text"
                value={addDepartment}
                onChange={(e) => setAddDepartment(e.target.value)}
                placeholder="Departman"
                className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <button
                type="submit"
                className="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600"
              >
                Ekle
              </button>
            </div>
          </form>
          {/* VERİ SATIRLARI */}
          <div className="bg-white">
            {filteredData.map((employee, rowIndex) => (
              <div key={employee.id} className={`grid grid-cols-8 gap-0 border-b border-gray-200 hover:bg-gray-50 ${rowIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.id }
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.name}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.surname}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.email}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.password}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.position}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee?.department}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm text-center">
                <button
                  className="px-3 py-1 text-xs bg-red-500 text-white rounded hover:bg-blue-600 transition-colors flex items-center"
                  onClick={() => handleRemove(employee.id)}
                >
                  <OctagonMinus className="w-4 h-4 mr-1" />
                  Kaldır
                </button>
              </div>
              </div>
            ))}
          </div>
        </div>    
      </div>
    </div>
  );
};

export default AdminAddUser;