import React, { useState } from 'react';
import user from "./EmployeeMain"

const EmployeeTeamAttendance = ({ user }) => {

  // Örnek data
  const teamData = [
    {
      id: 1,
      name: "AHMET EMIN",
      surname: "KAHRAMAN",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [true, true, true, true, true], // Pzt, Salı, Çrş, Perş, Cuma
      isApproved: false,
      employeeExcuse: "Doktor raporu mevcut - Perşembe günü kontrole gideceğim"
    },
    {
      id: 2,
      name: "EMILETTIN YAVUZ",
      surname: "ÜVE",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [true, true, false, false, true],
      isApproved: true,
      employeeExcuse: ""
    },
    {
      id: 3,
      name: "CENK",
      surname: "KARAASLAN",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [false, true, true, true, false],
      isApproved: false,
      employeeExcuse: "Cuma günü özel işim var, pazartesi evden çalışacağım"
    },
    {
      id: 4,
      name: "ONUR",
      surname: "ÇİMEN",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [true, false, false, false, true],
      isApproved: true,
      employeeExcuse: ""
    }
  ];

  const [teamState, setTeamState] = useState(teamData);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingMember, setEditingMember] = useState(null);
  const [tempAttendance, setTempAttendance] = useState([]);
  const [editReason, setEditReason] = useState('');

  const generateWeekDays = () => {
    const today = new Date();
    const currentWeekStart = new Date(today);
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1);
    currentWeekStart.setDate(diff);
    
    const weekDays = [];
    for (let i = 0; i < 5; i++) {
      const date = new Date(currentWeekStart);
      date.setDate(currentWeekStart.getDate() + i);
      weekDays.push(date);
    }
    return weekDays;
  };

  const weekDays = generateWeekDays();
  
  // Rol kontrolü
  const isTeamLeader = user?.authorities?.includes('ROLE_attendance_client_team_leader');
  console.log(isTeamLeader);

  // Onaylama fonksiyonu
  const handleApprove = (memberId) => {
    setTeamState(prevState => 
      prevState.map(member => 
        member.id === memberId 
          ? { ...member, isApproved: true }
          : member
      )
    );
  };

  // Düzenleme fonksiyonu - Modal açma
  const handleEdit = (memberId) => {
    const member = teamState.find(m => m.id === memberId);
    setEditingMember(member);
    setTempAttendance([...member.attendance]);
    setEditReason(''); // Düzenleme sebebi başlangıçta boş
    setIsEditModalOpen(true);
  };

  // Modal'daki değişiklikleri kaydetme
  const handleSaveChanges = () => {
    if (editingMember && editReason.trim()) {
      setTeamState(prevState => 
        prevState.map(member => 
          member.id === editingMember.id 
            ? { 
                ...member, 
                attendance: [...tempAttendance], 
                isApproved: false, // Düzenlenen kayıt onayını kaybeder
                lastEditReason: editReason,
                lastEditDate: new Date().toLocaleDateString('tr-TR')
              } 
            : member
        )
      );
      setIsEditModalOpen(false);
      setEditingMember(null);
      setTempAttendance([]);
      setEditReason('');
      
      // Başarı mesajı (isteğe bağlı)
      alert('Değişiklikler kaydedildi!');
    } else if (!editReason.trim()) {
      alert('Lütfen düzenleme sebebini belirtin.');
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
  const handleTempAttendanceChange = (dayIndex) => {
    const newAttendance = [...tempAttendance];
    newAttendance[dayIndex] = !newAttendance[dayIndex];
    setTempAttendance(newAttendance);
  };

  return (
    <div className="flex-1 p-6 bg-white">

      {/* Düzenleme Modal'ı */}
      {isEditModalOpen && editingMember && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl mx-4">
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
                  Çalışanın Mazereti
                </label>
                <div className="bg-gray-50 border rounded-lg p-3">
                  {editingMember.employeeExcuse ? (
                    <p className="text-gray-800 text-sm">{editingMember.employeeExcuse}</p>
                  ) : (
                    <p className="text-gray-500 text-sm italic">Mazeret belirtilmemiş</p>
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
              <div className="grid grid-cols-5 gap-4">
                {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, index) => (
                  <div key={index} className="text-center">
                    <div className="text-sm font-medium text-gray-600 mb-2">{dayName}</div>
                    <div className="text-xs text-gray-500 mb-3">
                      {weekDays[index].toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                    </div>
                    
                    {/* Ofiste/Uzaktan Seçimi */}
                    <div className="space-y-2">
                      <label className="flex items-center justify-center cursor-pointer">
                        <input
                          type="radio"
                          name={`attendance-${index}`}
                          checked={tempAttendance[index] === true}
                          onChange={() => setTempAttendance(prev => {
                            const newAttendance = [...prev];
                            newAttendance[index] = true;
                            return newAttendance;
                          })}
                          className="sr-only"
                        />
                        <div className={`w-8 h-8 rounded-full border-2 flex items-center justify-center ${
                          tempAttendance[index] === true 
                            ? 'bg-green-500 border-green-500' 
                            : 'border-gray-300 hover:border-green-400'
                        }`}>
                          {tempAttendance[index] === true && (
                            <div className="w-4 h-4 bg-white rounded-full"></div>
                          )}
                        </div>
                      </label>
                      <div className="text-xs text-gray-600">Ofiste</div>
                      
                      <label className="flex items-center justify-center cursor-pointer">
                        <input
                          type="radio"
                          name={`attendance-${index}`}
                          checked={tempAttendance[index] === false}
                          onChange={() => setTempAttendance(prev => {
                            const newAttendance = [...prev];
                            newAttendance[index] = false;
                            return newAttendance;
                          })}
                          className="sr-only"
                        />
                        <div className={`w-8 h-8 rounded-full border-2 ${
                          tempAttendance[index] === false 
                            ? 'border-gray-400 bg-white' 
                            : 'border-gray-300 hover:border-gray-400'
                        }`}>
                        </div>
                      </label>
                      <div className="text-xs text-gray-600">Uzaktan</div>
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
                  className={`px-4 py-2 rounded transition-colors ${
                    editReason.trim()
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
        <h2 className="text-xl mb-2">Ekibinizin ofis günleri</h2>
        <div className="flex items-center gap-2 mb-4">
        </div>
      </div>

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
              {isTeamLeader && (
                <th className="text-center p-3 font-medium text-gray-700 min-w-[120px]">
                  İşlemler
                </th>
              )}
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
                  {member.isApproved && (
                    <div className="text-xs text-green-600 flex items-center gap-1 mt-1">
                      <span>✓</span>
                      <span>Onaylandı</span>
                    </div>
                  )}
                </td>
                {member.attendance.map((isPresent, dayIndex) => (
                  <td key={dayIndex} className="p-3 border-r border-b text-center">
                    <div className="flex justify-center">
                      {isPresent ? (
                        <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                          <div className="w-3 h-3 bg-white rounded-full"></div>
                        </div>
                      ) : (
                        <div className="w-6 h-6 border-2 border-gray-400 rounded-full bg-white"></div>
                      )}
                    </div>
                  </td>
                ))}
                {isTeamLeader && (
                  <td className="p-3 border-b text-center">
                    <div className="flex gap-2 justify-center">
                      <button
                        onClick={() => handleEdit(member.id)}
                        className="px-3 py-1 text-xs bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                      >
                        Düzenle
                      </button>
                      {!member.isApproved && (
                        <button
                          onClick={() => handleApprove(member.id)}
                          className="px-3 py-1 text-xs bg-green-500 text-white rounded hover:bg-green-600 transition-colors"
                        >
                          Onayla
                        </button>
                      )}
                      {member.isApproved && (
                        <span className="px-3 py-1 text-xs bg-gray-200 text-gray-600 rounded">
                          Onaylandı
                        </span>
                      )}
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 text-sm text-gray-600">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-green-500 rounded-full flex items-center justify-center">
              <div className="w-2 h-2 bg-white rounded-full"></div>
            </div>
            <span>Ofiste</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border-2 border-gray-400 rounded-full bg-white"></div>
            <span>Uzaktan</span>
          </div>
          {isTeamLeader && (
            <div className="flex items-center gap-2 ml-4">
              <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                ✓ Onaylandı
              </span>
              <span>durumu gösterir</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default EmployeeTeamAttendance;