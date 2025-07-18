import React, { useState } from 'react';
import { Save } from 'lucide-react';
import Swal from 'sweetalert2';

const EmployeeAttendanceRegistration = () => {
  const [selectedDates, setSelectedDates] = useState([]);
  const minDay = 2; //departmana göre değişecek
  const generateWeekDays = () => {
    const today = new Date();
    const currentWeekStart = new Date(today);
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Sunday
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

  const handleDateToggle = (date) => {
    const dateStr = date.toISOString().split('T')[0];
    setSelectedDates(prev => 
      prev.includes(dateStr) 
        ? prev.filter(d => d !== dateStr)
        : [...prev, dateStr]
    );
  };

  const isDateSelected = (date) => {
    return selectedDates.includes(date.toISOString().split('T')[0]);
  };

  const handleSave = () => {
    if (selectedDates.length < minDay) {
      Swal.fire({
        title: 'Emin misiniz?',
        text: `En az ${minDay} gün seçmeniz beklenmektedir.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Evet',
        cancelButtonText: 'Hayır',
      }).then((result) => {
        if (result.isConfirmed) {
          console.log('Bilgiler kaydedildi!');
        }
      });
      return;
    }
    else{
      Swal.fire({
        title: 'Emin misiniz?',
        text: "Günler kaydedilecektir.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Evet',
        cancelButtonText: 'Hayır',
      }).then((result) => {
        if (result.isConfirmed) {
          console.log('Bilgiler kaydedildi!');
        }
      });
      return;
    }
  };

  return (
    <div className="flex-1 p-6 bg-white">
      <div className="mb-6">

        <div className="mb-6">
          <h3 className="text-lg font-semibold mb-4">Bu Hafta - İş Günleri</h3>
          
          <div className="grid grid-cols-5 gap-4 mb-6">
            {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, index) => (
              <div key={index} className="text-center">
                <div className="text-sm font-medium text-gray-600 mb-2">{dayName}</div>
                <div className="text-xs text-gray-500 mb-3">
                  {weekDays[index].toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                </div>
                <div
                  onClick={() => handleDateToggle(weekDays[index])}
                  className={`
                    p-4 cursor-pointer rounded-lg border-2 transition-all h-20 flex items-center justify-center
                    ${isDateSelected(weekDays[index]) 
                      ? 'bg-green-500 text-white border-green-500 shadow-lg' 
                      : 'bg-white border-gray-200 hover:border-gray-300 hover:shadow-md'
                    }
                  `}
                >
                  {isDateSelected(weekDays[index]) && (
                    <div className="text-center">
                      <div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1">
                        <div className="w-4 h-4 bg-green-500 rounded-full"></div>
                      </div>
                      <div className="text-xs font-medium">Seçili</div>
                    </div>
                  )}
                  {!isDateSelected(weekDays[index]) && (
                    <div className="text-center">
                      <div className="w-8 h-8 border-2 border-gray-300 rounded-full mx-auto mb-1"></div>
                      <div className="text-xs text-gray-500">Seç</div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {selectedDates.length > 0 && (
          <div className={`mb-4 p-4 rounded-lg ${selectedDates.length >= minDay ? 'bg-green-50' : 'bg-red-50'}`}>
            <p className={`text-sm ${selectedDates.length >= minDay ? 'text-green-700' : 'text-red-700'}`}>
              <strong>Seçilen günler:</strong> {selectedDates.length} gün
            </p>
            <p className={`text-xs mt-1 ${selectedDates.length >= minDay ? 'text-green-600' : 'text-red-600'}`}>
              {selectedDates.length >= minDay 
                ? 'Kaydetmeye hazır!' 
                : `En az ${minDay - selectedDates.length} gün daha seçmelisiniz!`
              }
            </p>
          </div>
        )}

        <button
          onClick={handleSave}
          
          className={`
            px-6 py-3 rounded-lg font-medium transition-all flex items-center gap-2 bg-blue-600 text-white hover:bg-blue-700
            }
          `}
        >
          <Save className="w-4 h-4" />
          Kaydet
        </button>
      </div>
    </div>
  );
};

export default EmployeeAttendanceRegistration;