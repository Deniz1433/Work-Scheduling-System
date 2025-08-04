import React, { useState, useEffect } from 'react';
import axios from 'axios';
import * as XLSX from 'xlsx';
import Select from 'react-select';
import Swal from 'sweetalert2';
const EmployeeTeamAttendance = ({ user }) => {

  const [teamState, setTeamState] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Filtreleme state'leri
  const [departments, setDepartments] = useState([]);
  const [roles, setRoles] = useState([]);
  const [selectedDepartments, setSelectedDepartments] = useState([]);
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');

  // Departman ve rol verilerini yükle
  const fetchFilterData = async () => {
    try {
      const [departmentsResponse, rolesResponse] = await Promise.all([
        fetch('/api/departments'),
        fetch('/api/roles')
      ]);
      
      const departmentsData = await departmentsResponse.json();
      const rolesData = await rolesResponse.json();
      
      setDepartments(departmentsData);
      setRoles(rolesData);
    } catch (err) {
      console.error('Filtre verileri yüklenemedi:', err);
    }
  };

  const fetchTeamData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Query parametrelerini oluştur
      const params = new URLSearchParams();
      if (selectedDepartments.length > 0) {
        const deptIds = selectedDepartments.map(dept => dept.value).join(',');
        params.append('departmentId', deptIds);
      }
      if (selectedRoles.length > 0) {
        const roleIds = selectedRoles.map(role => role.value).join(',');
        params.append('roleId', roleIds);
      }
      if (searchTerm) params.append('searchTerm', searchTerm);

      const response = await fetch(`/api/attendance/team?${params.toString()}`);
      const data = await response.json();
      setTeamState(data);
    }
    catch (err) {
      setError('Veri alınamadı.');
    } finally {
      setLoading(false);
    }
  };

  // Filtreleri temizle
  const clearFilters = () => {
    setSelectedDepartments([]);
    setSelectedRoles([]);
    setSearchTerm('');
  };

  // Excel export fonksiyonu
  const exportToExcel = () => {
    if (teamState.length === 0) {
      alert('Export edilecek veri bulunamadı!');
      return;
    }

    try {
      // Excel için veri hazırlama
      const excelData = teamState.map((member, index) => {
        const row = {
          'Sıra': index + 1,
          'Ad': member.name,
          'Soyad': member.surname,
          'Departman': member.department,
          'Pazartesi': getAttendanceLabel(member.attendance[0]),
          'Salı': getAttendanceLabel(member.attendance[1]),
          'Çarşamba': getAttendanceLabel(member.attendance[2]),
          'Perşembe': getAttendanceLabel(member.attendance[3]),
          'Cuma': getAttendanceLabel(member.attendance[4]),
          'Onay Durumu': (member.approved || member.isApproved) ? 'Onaylandı' : 'Onaylanmadı'
        };
        return row;
      });

      // Hafta bilgisi
      const weekInfo = {
        'Hafta Başlangıcı': weekDays[0].toLocaleDateString('tr-TR'),
        'Hafta Bitişi': weekDays[4].toLocaleDateString('tr-TR'),
        'Export Tarihi': new Date().toLocaleDateString('tr-TR'),
        'Toplam Kişi': teamState.length
      };

             // Filtre bilgileri
       const filterInfo = {
         'Departman Filtresi': selectedDepartments.length > 0 
           ? selectedDepartments.map(d => d.label).join(', ') 
           : 'Tüm Departmanlar',
         'Rol Filtresi': selectedRoles.length > 0 
           ? selectedRoles.map(r => r.label).join(', ') 
           : 'Tüm Roller',
         'Arama Terimi': searchTerm || 'Yok'
       };

      // Workbook oluşturma
      const wb = XLSX.utils.book_new();

      // Ana veri sayfası
      const ws = XLSX.utils.json_to_sheet(excelData);
      XLSX.utils.book_append_sheet(wb, ws, 'Takım Devam Durumu');

      // Bilgi sayfası
      const infoData = [
        { 'Bilgi': 'Hafta Bilgileri', 'Değer': '' },
        { 'Bilgi': 'Hafta Başlangıcı', 'Değer': weekInfo['Hafta Başlangıcı'] },
        { 'Bilgi': 'Hafta Bitişi', 'Değer': weekInfo['Hafta Bitişi'] },
        { 'Bilgi': 'Export Tarihi', 'Değer': weekInfo['Export Tarihi'] },
        { 'Bilgi': 'Toplam Kişi', 'Değer': weekInfo['Toplam Kişi'] },
        { 'Bilgi': '', 'Değer': '' },
        { 'Bilgi': 'Filtre Bilgileri', 'Değer': '' },
        { 'Bilgi': 'Departman Filtresi', 'Değer': filterInfo['Departman Filtresi'] },
        { 'Bilgi': 'Rol Filtresi', 'Değer': filterInfo['Rol Filtresi'] },
        { 'Bilgi': 'Arama Terimi', 'Değer': filterInfo['Arama Terimi'] }
      ];
      const wsInfo = XLSX.utils.json_to_sheet(infoData);
      XLSX.utils.book_append_sheet(wb, wsInfo, 'Bilgiler');

      // Durum açıklamaları sayfası
      const statusInfo = [
        { 'Durum Kodu': 0, 'Açıklama': 'Veri Yok' },
        { 'Durum Kodu': 1, 'Açıklama': 'Ofiste' },
        { 'Durum Kodu': 2, 'Açıklama': 'Uzaktan' },
        { 'Durum Kodu': 3, 'Açıklama': 'İzinli' },
        { 'Durum Kodu': 4, 'Açıklama': 'Mazeretli' },
        { 'Durum Kodu': 5, 'Açıklama': 'Resmi Tatil' }
      ];
      const wsStatus = XLSX.utils.json_to_sheet(statusInfo);
      XLSX.utils.book_append_sheet(wb, wsStatus, 'Durum Açıklamaları');

      // Dosya adı oluşturma
      const fileName = `devam_durumu_${weekDays[0].toISOString().split('T')[0]}_${new Date().toISOString().split('T')[0]}.xlsx`;

      // Excel dosyasını indirme
      XLSX.writeFile(wb, fileName);
      
      // Başarı mesajı
      Swal.fire({
        title: 'Başarılı',
        text: `Excel dosyası başarıyla indirildi: ${fileName}`,
        icon: 'success'
      });
    } catch (error) {
      console.error('Excel export hatası:', error);
      Swal.fire({
        title: 'Hata',
        text: 'Excel dosyası oluşturulurken hata oluştu!',
        icon: 'error'
      });
    }
  };

  // Component mount olduğunda filtre verilerini yükle ve tüm kullanıcıları getir
  useEffect(() => {
    fetchFilterData();
  }, []);



  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingMember, setEditingMember] = useState(null);
  const [tempAttendance, setTempAttendance] = useState([]);
  const [editReason, setEditReason] = useState('');
  const [employeeExcuses, setEmployeeExcuses] = useState([]);

  const generateWeekDays = () => {
    const today = new Date();
    const nextWeekStart = new Date(today);
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1) + 7; // +7 for next week
    nextWeekStart.setDate(diff);

    const weekDays = [];
    for (let i = 0; i < 5; i++) {
      const date = new Date(nextWeekStart);
      date.setDate(nextWeekStart.getDate() + i);
      weekDays.push(date);
    }
    return weekDays;
  };

  const weekDays = generateWeekDays();

  // Devam durumu stillerini tanımlama
  const getAttendanceStyle = (status) => {
    const styles = {
      0: { // Veri girişi yok
        bg: 'bg-gray-200',
        border: 'border-gray-300',
        inner: 'bg-gray-400',
        text: 'text-gray-600',
        icon: '?'
      },
      1: { // Ofiste
        bg: 'bg-green-500',
        border: 'border-green-500',
        inner: 'bg-white',
        text: 'text-green-600',
        icon: '●'
      },
      2: { // Uzaktan
        bg: 'bg-blue-500',
        border: 'border-blue-500',
        inner: 'bg-white',
        text: 'text-blue-600',
        icon: '🏠'
      },
      3: { // İzinli
        bg: 'bg-yellow-500',
        border: 'border-yellow-500',
        inner: 'bg-white',
        text: 'text-yellow-600',
        icon: '📅'
      },
      4: { // Mazeretli
        bg: 'bg-purple-500',
        border: 'border-purple-500',
        inner: 'bg-white',
        text: 'text-orange-600',
        icon: '⚠'
      },
      5: { // Resmi Tatil
        bg: 'bg-orange-500',
        border: 'border-orange-500',
        inner: 'bg-white',
        text: 'text-purple-600',
        icon: '🎉'
      }
    };
    return styles[status] || styles[0];
  };

  // Devam durumu label'ları
  const getAttendanceLabel = (status) => {
    const labels = {
      0: 'Veri Yok',
      1: 'Ofiste',
      2: 'Uzaktan',
      3: 'İzinli',
      4: 'Mazeretli',
      5: 'Resmi Tatil'
    };
    return labels[status] || 'Bilinmiyor';
  };

  // Onaylama fonksiyonu
  const handleApprove = async (memberId) => {
    try {
      const response = await axios.post(`/api/attendance/${memberId}/approve`);
      console.log(response);

      if (employeeExcuses.length > 0) {
        for (const excuse of employeeExcuses) {
          if (excuse.approved === false || excuse.isApproved === false) {
            const responseExcuseApprove = await axios.post(`/api/attendance/excuse/${excuse.id}/approve`);
            console.log(responseExcuseApprove);
          }
        }
      }
      // Başarılı onaylama sonrası verileri yeniden çek
      await fetchTeamData();

    } catch (error) {
      console.error('API Error:', error);
      alert('Onaylama işlemi sırasında hata oluştu: ' + (error.response?.data?.message || error.message));
      return;
    }
  };

  // Düzenleme fonksiyonu - Modal açma
  const handleEdit = async (memberId) => {
    const member = teamState.find(m => m.id === memberId);
    setEditingMember(member);
    setTempAttendance([...member.attendance]);
    const excuseInfo = await fetch(`/api/attendance/excuse/${memberId}`);
    const excuseData = await excuseInfo.json();
    setEmployeeExcuses(excuseData);
    console.log(excuseData);
    setEditReason(''); // Düzenleme sebebi başlangıçta boş
    setIsEditModalOpen(true);
  };

  // Modal'daki değişiklikleri kaydetme
  const handleSaveChanges = async () => {
    try {
      console.log('Sending request with data:', {
        userId: editingMember.id.toString(),
        weekStart: weekDays[0].toISOString().split('T')[0],
        dates: tempAttendance
      });

      const response = await axios.post(`/api/attendance/${editingMember.id}`, {
        userId: editingMember.id.toString(),
        weekStart: weekDays[0].toISOString().split('T')[0],
        dates: tempAttendance
      });
      if(employeeExcuses.length > 0){
        for(let i = 0; i < 5; i++){
          if((editingMember.attendance[i] === 3 || editingMember.attendance[i] === 4) && tempAttendance[i] === 1 || tempAttendance[i] === 2){
            console.log(employeeExcuses[i]);
            const responseExcuseDelete = await axios.delete(`/api/excuse/${employeeExcuses[i].id}`);
            console.log(responseExcuseDelete);
          }
        }
      }

      console.log('API Response:', response);
      console.log('Response data:', response.data);
      console.log('Response status:', response.status);

    } catch (error) {
      console.error('API Error:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      alert('Değişiklikler kaydedilirken hata oluştu: ' + (error.response?.data?.message || error.message));
      return; // Hata durumunda işlemi durdur
    }

    if (editingMember && editReason.trim()) {
      // Başarılı düzenleme sonrası verileri yeniden çek
      await fetchTeamData();

      setIsEditModalOpen(false);
      setEditingMember(null);
      setTempAttendance([]);
      setEditReason('');

      // Başarı mesajı
      Swal.fire({
        title: 'Başarılı',
        text: 'Değişiklikler kaydedildi!',
        icon: 'success'
      });
    } else if (!editReason.trim()) {
      Swal.fire({
        title: 'Hata',
        text: 'Lütfen düzenleme sebebini belirtin.',
        icon: 'error'
      });
    }
  };

  // Modal'ı iptal etme
  const handleCancelEdit = () => {
    setIsEditModalOpen(false);
    setEditingMember(null);
    setTempAttendance([]);
    setEditReason('');
  };

  // Temporary attendance değişikliği
  const handleTempAttendanceChange = (dayIndex, newStatus) => {
    const newAttendance = [...tempAttendance];
    newAttendance[dayIndex] = newStatus;
    setTempAttendance(newAttendance);
  };

  if (loading) {
    return (
      <div className="flex-1 p-6 bg-white">
        <div className="flex items-center justify-center h-64">
          <div className="text-gray-600">Yükleniyor...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-1 p-6 bg-white">
        <div className="flex items-center justify-center h-64">
          <div className="text-red-600">{error}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 p-6 bg-white">
      {/* Filtreleme Bölümü */}
      <div className="mb-6 bg-gray-50 p-4 rounded-lg border">
                 <h3 className="text-lg font-medium text-gray-800 mb-4">Sorgu Seçenekleri</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
                     {/* Departman Filtresi */}
           <div>
             <label className="block text-sm font-medium text-gray-700 mb-2">
               Departman
             </label>
             <Select
               isMulti
               value={selectedDepartments}
               onChange={setSelectedDepartments}
               options={departments.map(dept => ({ value: dept.id, label: dept.name }))}
               placeholder="Departman seçin..."
               className="text-sm"
               classNamePrefix="select"
               noOptionsMessage={() => "Departman bulunamadı"}
               isClearable={true}
               isSearchable={true}
               styles={{
                 control: (provided, state) => ({
                   ...provided,
                   borderColor: state.isFocused ? '#3b82f6' : '#d1d5db',
                   boxShadow: state.isFocused ? '0 0 0 1px #3b82f6' : 'none',
                   '&:hover': {
                     borderColor: '#3b82f6'
                   }
                 }),
                 multiValue: (provided) => ({
                   ...provided,
                   backgroundColor: '#3b82f6',
                   color: 'white'
                 }),
                 multiValueLabel: (provided) => ({
                   ...provided,
                   color: 'white'
                 }),
                 multiValueRemove: (provided) => ({
                   ...provided,
                   color: 'white',
                   '&:hover': {
                     backgroundColor: '#2563eb',
                     color: 'white'
                   }
                 })
               }}
             />
           </div>

           {/* Rol Filtresi */}
           <div>
             <label className="block text-sm font-medium text-gray-700 mb-2">
               Rol
             </label>
             <Select
               isMulti
               value={selectedRoles}
               onChange={setSelectedRoles}
               options={roles.map(role => ({ value: role.id, label: role.name }))}
               placeholder="Rol seçin..."
               className="text-sm"
               classNamePrefix="select"
               noOptionsMessage={() => "Rol bulunamadı"}
               isClearable={true}
               isSearchable={true}
               styles={{
                 control: (provided, state) => ({
                   ...provided,
                   borderColor: state.isFocused ? '#3b82f6' : '#d1d5db',
                   boxShadow: state.isFocused ? '0 0 0 1px #3b82f6' : 'none',
                   '&:hover': {
                     borderColor: '#3b82f6'
                   }
                 }),
                 multiValue: (provided) => ({
                   ...provided,
                   backgroundColor: '#3b82f6',
                   color: 'white'
                 }),
                 multiValueLabel: (provided) => ({
                   ...provided,
                   color: 'white'
                 }),
                 multiValueRemove: (provided) => ({
                   ...provided,
                   color: 'white',
                   '&:hover': {
                     backgroundColor: '#2563eb',
                     color: 'white'
                   }
                 })
               }}
             />
           </div>

          {/* Arama Filtresi */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Arama
            </label>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="İsim, soyisim veya email..."
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

        
        </div>

        {/* Filtre Butonları */}
        <div className="flex gap-3">
          <button
            onClick={clearFilters}
            className="px-4 py-2 text-sm bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
          >
            Filtreleri Temizle
          </button>
          <button
            onClick={() => {
              fetchTeamData();
            }}
            className={`px-4 py-2 text-sm rounded-lg transition-colors bg-blue-500 text-white hover:bg-blue-600`}>
            Verileri Getir
          </button>
        </div>
      </div>
      {/* Düzenleme Modal'ı */}
      {isEditModalOpen && editingMember && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-3xl mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-semibold text-gray-800">
                Devam Durumu Düzenle
              </h3>
              <button
                onClick={handleCancelEdit}
                className="text-gray-400 hover:text-gray-600 text-xl"
              >
                ×
              </button>
            </div>

            {/* Çalışan Bilgileri */}
            <div className="bg-gray-50 p-4 rounded-lg mb-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                  <span className="text-blue-600">👤</span>
                </div>
                <div>
                  <div className="font-medium text-gray-800">
                    {editingMember.name} {editingMember.surname}
                  </div>
                  <div className="text-sm text-gray-600">{editingMember.department}</div>
                </div>
              </div>
            </div>

            {/* Mazeret ve Düzenleme Sebebi Alanları */}
            <div className="space-y-4 mb-6">
              {/* Çalışandan Gelen Mazeret */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Çalışanın Mazeretleri
                </label>
                <div className="bg-gray-50 border rounded-lg p-3">
                  {employeeExcuses.length > 0 && employeeExcuses.map((excuse) => (
                    <div key={excuse.id}>
                      <p className="text-gray-800 text-sm">{excuse.excuseDate}</p>
                      <p className="text-gray-800 text-sm">{excuse.description}</p>
                    </div>
                  ))}
                  {employeeExcuses.length === 0 && (
                    <p className="text-gray-500 text-sm italic">Mazeret Yok</p>
                  )}
                </div>
              </div>

              {/* Düzenleme Sebebi */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Düzenleme Sebebi <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={editReason}
                  onChange={(e) => setEditReason(e.target.value)}
                  placeholder="Düzenleme nedenini açıklayın..."
                  rows={3}
                  className="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Bu açıklama çalışana gönderilecektir.
                </p>
              </div>
            </div>

            {/* Hafta Günleri Düzenleme */}
            <div className="mb-6">
              <h4 className="text-md font-medium text-gray-800 mb-4">Haftalık Devam Durumu</h4>
              <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, index) => (
                  <div key={index} className="border rounded-lg p-4">
                    <div className="text-sm font-medium text-gray-800 mb-2 text-center">{dayName}</div>
                    <div className="text-xs text-gray-500 mb-3 text-center">
                      Mevcut durum: {getAttendanceLabel(tempAttendance[index])}
                    </div>
                    <div className="text-xs text-gray-500 mb-3 text-center">
                      {weekDays[index].toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                    </div>

                    {/* Durum Seçenekleri */}
                    <div className="space-y-3">{
                      tempAttendance[index] != 5 && (
                        [
                          { value: 1, label: 'Ofiste', color: 'green' },
                          { value: 2, label: 'Uzaktan', color: 'blue' },
                        ].map((option) => (
                          <label key={option.value} className="flex items-center gap-2 cursor-pointer">
                            <input
                              type="radio"
                              name={`attendance-${index}`}
                              checked={tempAttendance[index] === option.value}
                              onChange={() => handleTempAttendanceChange(index, option.value)}
                              className="sr-only"
                            />
                            <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${tempAttendance[index] === option.value
                              ? `bg-${option.color}-500 border-${option.color}-500`
                              : `border-${option.color}-300 hover:border-${option.color}-400`
                              }`}>
                              {tempAttendance[index] === option.value && (
                                <div className="w-2 h-2 bg-white rounded-full"></div>
                              )}
                            </div>
                            <span className={`text-xs ${tempAttendance[index] === option.value
                              ? `text-${option.color}-700 font-medium`
                              : 'text-gray-600'
                              }`}>
                              {option.label}
                            </span>
                          </label>
                        )))}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Modal Alt Butonları */}
            <div className="flex justify-between items-center">
              <div className="text-sm text-gray-500">
                <span className="text-red-500">*</span> Zorunlu alanlar
              </div>
              <div className="flex gap-3">
                <button
                  onClick={handleCancelEdit}
                  className="px-4 py-2 text-gray-600 border border-gray-300 rounded hover:bg-gray-50 transition-colors"
                >
                  İptal
                </button>
                <button
                  onClick={handleSaveChanges}
                  disabled={!editReason.trim()}
                  className={`px-4 py-2 rounded transition-colors ${editReason.trim()
                    ? 'bg-blue-500 text-white hover:bg-blue-600'
                    : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                    }`}
                >
                  Değişiklikleri Kaydet
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
                     <h2 className="text-xl">
             {selectedDepartments.length > 0 || selectedRoles.length > 0 || searchTerm.trim()
               ? 'Filtrelenmiş Takım Verileri'
               : 'Tüm Takım Verileri'
             }
           </h2>
          <div className="flex items-center gap-4">
            <div className="text-sm text-gray-600">
              {teamState.length} kişi bulundu
            </div>
            {teamState.length > 0 && (
              <button
                onClick={exportToExcel}
                className="px-4 py-2 text-sm bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors flex items-center gap-2"
              >
                <span>📊</span>
                Excel'e Aktar
              </button>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2 mb-4">
          {user?.departmentId && (
            <span className="text-sm text-gray-600">
              Departman ID: {user.departmentId}
            </span>
          )}
        </div>
      </div>

      {teamState.length === 0 ? (
        <div className="bg-white border rounded-lg p-8 text-center">
          <div className="text-gray-500 mb-4">
            <div className="text-4xl mb-2">🔍</div>
                         <div className="text-lg font-medium">
               {selectedDepartments.length > 0 || selectedRoles.length > 0 || searchTerm.trim() 
                 ? 'Filtreleme kriterlerine uygun kişi bulunamadı' 
                 : 'Veri yüklenmedi'
               }
             </div>
             <div className="text-sm">
               {selectedDepartments.length > 0 || selectedRoles.length > 0 || searchTerm.trim()
                 ? 'Farklı filtre seçenekleri deneyebilirsiniz.'
                 : 'Verileri Getir butonuna tıklayarak kullanıcıları görüntüleyebilirsiniz.'
               }
             </div>
          </div>
        </div>
      ) : (
        <div className="bg-white border rounded-lg overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="bg-gray-100">
                <th className="text-left p-3 font-medium text-gray-700 border-r">Sıra</th>
                <th className="text-left p-3 font-medium text-gray-700 border-r">Soyadı</th>
                <th className="text-left p-3 font-medium text-gray-700 border-r">Departman</th>
                {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, index) => (
                  <th key={index} className="text-center p-3 font-medium text-gray-700 border-r min-w-[80px]">
                    <div className="text-xs text-gray-600">{dayName}</div>
                    <div className="text-sm font-semibold">{weekDays[index].toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}</div>
                  </th>
                ))}
                <th className="text-center p-3 font-medium text-gray-700 min-w-[120px]">
                  İşlemler
                </th>
              </tr>
            </thead>
            <tbody>
              {teamState.map((member, memberIndex) => (
                <tr key={member.id} className={memberIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'}>
                  <td className="p-3 border-r border-b">
                    <div className="flex items-center gap-2">
                      <span className="text-gray-600">{memberIndex + 1}</span>
                      <div className="w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
                        <span className="text-xs text-blue-600">👤</span>
                      </div>
                    </div>
                  </td>
                  <td className="p-3 border-r border-b">
                    <div className="font-medium text-gray-800">{member.name}</div>
                    <div className="text-sm text-gray-600">{member.surname}</div>
                  </td>
                  <td className="p-3 border-r border-b">
                    <div className="text-sm text-gray-700">{member.department}</div>

                    {(member.approved || member.isApproved) && (
                      <div className="text-xs text-green-600 flex items-center gap-1 mt-1">
                        <span>✓</span>
                        <span>Onaylandı</span>
                      </div>
                    )}
                  </td>
                  {member.attendance.map((status, dayIndex) => {
                    const style = getAttendanceStyle(status);
                    return (
                      <td key={dayIndex} className="p-3 border-r border-b text-center">
                        <div className="flex flex-col items-center gap-1">
                          <div className={`w-6 h-6 rounded-full border-2 ${style.bg} ${style.border} flex items-center justify-center`}>

                            <div className={`w-3 h-3 ${style.inner} rounded-full`}></div>

                          </div>
                          <div className="text-xs text-gray-500">
                            {getAttendanceLabel(status)}
                          </div>
                        </div>
                      </td>
                    );
                  })}
                  <td className="p-3 border-b text-center">
                    <div className="flex gap-2 justify-center">
                      <button
                        onClick={() => handleEdit(member.id)}
                        className="px-3 py-1 text-xs bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                      >
                        Düzenle
                      </button>
                      {!(member.approved || member.isApproved) && (
                        <button
                          onClick={() => handleApprove(member.id)}
                          className="px-3 py-1 text-xs bg-green-500 text-white rounded hover:bg-green-600 transition-colors"
                        >
                          Onayla
                        </button>
                      )}
                      {(member.approved || member.isApproved) && (
                        <span className="px-3 py-1 text-xs bg-gray-200 text-gray-600 rounded">
                          Onaylandı
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Gösterge/Legend */}
      <div className="mb-6 p-4 rounded-lg bg-blue-50 border border-blue-200">
        <h4 className="text-sm font-medium text-blue-800 mb-3">Durum Açıklamaları:</h4>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-green-500 rounded-full"></div>
            <span className="text-green-700">Ofiste çalışma</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span className="text-blue-700">Uzaktan çalışma</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
            <span className="text-yellow-700">İzinli (Açıklama gerekli)</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-purple-500 rounded-full"></div>
            <span className="text-purple-700">Mazeretli (Açıklama gerekli)</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
            <span className="text-orange-700">Resmi Tatil (Değiştirilemez)</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmployeeTeamAttendance;