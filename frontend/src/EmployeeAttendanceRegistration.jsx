// frontend/src/EmployeeAttendanceRegistration.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Save } from 'lucide-react';
import Swal from 'sweetalert2';

const getIstanbulNow = () => {
    const now = new Date();
    const parts = new Intl.DateTimeFormat("en", {
        timeZone: "Europe/Istanbul",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false
    }).formatToParts(now);
    const m = {};
    parts.forEach(p => { if (p.type !== 'literal') m[p.type] = p.value; });
    return new Date(`${m.year}-${m.month}-${m.day}T${m.hour}:${m.minute}:${m.second}`);
};

const EmployeeAttendanceRegistration = () => {
    const [selectedDates, setSelectedDates] = useState([]);
    const [excusedDates, setExcusedDates] = useState([]);
    const minDay = 2;

    // Next week's Monday→Friday
    const weekDays = (() => {
        const today = getIstanbulNow();
        const day = today.getDay();
        const currentMonday = new Date(today);
        currentMonday.setDate(today.getDate() - day + (day === 0 ? -6 : 1));
        const nextMonday = new Date(currentMonday);
        nextMonday.setDate(currentMonday.getDate() + 7);
        return Array.from({ length: 5 }, (_, i) => {
            const d = new Date(nextMonday);
            d.setDate(nextMonday.getDate() + i);
            return d;
        });
    })();
    const weekDayStrings = weekDays.map(d => d.toISOString().split('T')[0]);

    useEffect(() => {
        axios.get('/api/attendance')
            .then(res => {
                const filtered = res.data.filter(date => weekDayStrings.includes(date));
                setSelectedDates(filtered);
            })
            .catch(err => console.error('Fetch attendance failed', err));

        axios.get('/api/excuse')
            .then(res => {
                const dates = res.data
                    .map(e => e.excuseDate)
                    .filter(date => weekDayStrings.includes(date));
                setExcusedDates(dates);
            })
            .catch(err => console.error('Fetch excuses failed', err));
    }, []); // only on mount

    const handleDateToggle = date => {
        const ds = date.toISOString().split('T')[0];
        if (excusedDates.includes(ds)) return;
        setSelectedDates(prev =>
            prev.includes(ds) ? prev.filter(d => d !== ds) : [...prev, ds]
        );
    };

    const isSelected = date =>
        selectedDates.includes(date.toISOString().split('T')[0]);
    const isExcused = date =>
        excusedDates.includes(date.toISOString().split('T')[0]);

    const effectiveDays = selectedDates.length + excusedDates.length;

    const handleSave = () => {
        const warningText =
            effectiveDays < minDay
                ? `Ofise en az ${minDay} gün gelmeniz gerekmektedir. Yine de kaydetmek istiyor musunuz?`
                : 'Seçiminiz kaydedilecektir.';

        Swal.fire({
            title: 'Emin misiniz?',
            text: warningText,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Evet',
            cancelButtonText: 'Hayır',
        }).then(result => {
            if (result.isConfirmed) {
                axios.post('/api/attendance', { dates: selectedDates })
                    .then(() => {
                        Swal.fire('Başarılı', 'Seçiminiz kaydedildi.', 'success');
                    })
                    .catch(() => {
                        Swal.fire('Hata', 'Kayıt sırasında bir sorun oluştu.', 'error');
                    });
            }
        });
    };

    return (
        <div className="flex-1 p-6 bg-white">
            <h3 className="text-lg font-semibold mb-4">Bu Hafta - İş Günleri</h3>

            <div className="grid grid-cols-5 gap-4 mb-6">
                {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, idx) => {
                    const date = weekDays[idx];
                    const ds = date.toISOString().split('T')[0];

                    if (isExcused(date)) {
                        return (
                            <div key={idx} className="text-center">
                                <div className="text-sm font-medium text-gray-600 mb-2">{dayName}</div>
                                <div className="text-xs text-gray-500 mb-3">
                                    {date.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                                </div>
                                <div className="p-4 h-20 flex items-center justify-center rounded-lg border-2 bg-green-100 text-green-700">
                                    <span className="text-xs font-medium">İzinli</span>
                                </div>
                            </div>
                        );
                    }

                    return (
                        <div key={idx} className="text-center">
                            <div className="text-sm font-medium text-gray-600 mb-2">{dayName}</div>
                            <div className="text-xs text-gray-500 mb-3">
                                {date.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                            </div>
                            <div
                                onClick={() => handleDateToggle(date)}
                                className={`
                  p-4 h-20 flex items-center justify-center cursor-pointer rounded-lg border-2 transition
                  ${
                                    isSelected(date)
                                        ? 'bg-green-500 text-white border-green-500 shadow-lg'
                                        : 'bg-white border-gray-200 hover:border-gray-300 hover:shadow-md'
                                }
                `}
                            >
                                {isSelected(date) ? (
                                    <div className="text-center">
                                        <div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1">
                                            <div className="w-4 h-4 bg-green-500 rounded-full"></div>
                                        </div>
                                        <div className="text-xs font-medium">Seçili</div>
                                    </div>
                                ) : (
                                    <div className="text-center">
                                        <div className="w-8 h-8 border-2 border-gray-300 rounded-full mx-auto mb-1"></div>
                                        <div className="text-xs text-gray-500">Seç</div>
                                    </div>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            {effectiveDays < minDay && (
                <div className="mb-4 p-4 rounded-lg bg-red-50">
                    <p className="text-xs text-red-600">
                        Ofise en az {minDay} gün gelmeniz gerekmektedir.
                    </p>
                </div>
            )}

            <button
                onClick={handleSave}
                className="px-6 py-3 rounded-lg font-medium transition-all flex items-center gap-2 bg-blue-600 text-white hover:bg-blue-700"
            >
                <Save className="w-4 h-4" />
                Kaydet
            </button>
        </div>
    );
};

export default EmployeeAttendanceRegistration;
