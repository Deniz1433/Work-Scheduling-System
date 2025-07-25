// frontend/src/EmployeeAttendanceRegistration.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Save } from 'lucide-react';
import Swal from 'sweetalert2';
import { useUser } from "./UserContext";

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
    const { user } = useUser();
    const [weeklyStatus, setWeeklyStatus] = useState([0, 0, 0, 0, 0]);
    const [debugLog, setDebugLog] = useState(''); // DEBUG state
    const minDay = 2;

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

    const weekStart = weekDays[0].toISOString().split('T')[0];

    const statusStyles = {
        0: { bg: 'bg-gray-100', border: 'border-gray-200', text: 'text-gray-500', hover: 'hover:border-gray-300' },
        1: { bg: 'bg-green-500', border: 'border-green-500', text: 'text-white', hover: '' },
        2: { bg: 'bg-blue-500', border: 'border-blue-500', text: 'text-white', hover: '' },
        3: { bg: 'bg-yellow-500', border: 'border-yellow-500', text: 'text-white', hover: '' },
        4: { bg: 'bg-purple-500', border: 'border-purple-500', text: 'text-white', hover: '' },
        5: { bg: 'bg-orange-500', border: 'border-orange-500', text: 'text-white', hover: '' }
    };

    useEffect(() => {
        if (!user) return;
        axios.get(`/api/attendance?weekStart=${weekStart}`)
            .then(res => {
                if (res.data && res.data.length === 5) {
                    setWeeklyStatus(res.data);
                }
            })
            .catch(err => {
                console.error('Fetch attendance failed', err);
                setDebugLog(`GET failed: ${err.message}`);
            });
    }, [user, weekStart]);

    const handleDayClick = (dayIndex) => {
        if (weeklyStatus[dayIndex] === 3 || weeklyStatus[dayIndex] === 4) return;

        setWeeklyStatus(prev => {
            const newStatus = [...prev];
            newStatus[dayIndex] = (newStatus[dayIndex] + 1) % 3;
            return newStatus;
        });
    };

    const officeDays = weeklyStatus.filter(status => status === 1).length;

    const handleSave = () => {
        if (weeklyStatus.includes(0)) {
            return Swal.fire('Eksik bilgi', 'Lütfen tüm günleri doldurunuz.', 'warning');
        }

        const warningText =
            officeDays < minDay
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
                axios.post('/api/attendance', {
                    weekStart,
                    dates: weeklyStatus
                }, {
                    withCredentials: true
                })
                    .then(() => {
                        setDebugLog('POST successful');
                        Swal.fire('Başarılı', 'Seçiminiz kaydedildi.', 'success');
                    })
                    .catch((err) => {
                        const errorMsg = err.response?.data?.message || err.message || 'Unknown error';
                        setDebugLog(`POST failed: ${errorMsg}`);
                        Swal.fire('Hata', 'Kayıt sırasında bir sorun oluştu.', 'error');
                    });
            }
        });
    };

    const renderDayCard = (dayName, dayIndex) => {
        const date = weekDays[dayIndex];
        const status = weeklyStatus[dayIndex];
        const style = statusStyles[status];
        const isClickable = status !== 3 && status !== 4;

        return (
            <div key={dayIndex} className="text-center">
                <div className="text-sm font-medium text-gray-600 mb-2">{dayName}</div>
                <div className="text-xs text-gray-500 mb-3">
                    {date.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                </div>
                <div
                    onClick={() => isClickable && handleDayClick(dayIndex)}
                    className={`p-4 h-20 flex items-center justify-center rounded-lg border-2 transition ${style.bg} ${style.border} ${style.text} ${isClickable ? `cursor-pointer ${style.hover} hover:shadow-md` : 'cursor-not-allowed'}`}
                >
                    <div className="text-center">
                        {status === 0 && <><div className="w-8 h-8 border-2 border-gray-300 rounded-full mx-auto mb-1"></div><div className="text-xs">Seç</div></>}
                        {status === 1 && <><div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1"><div className="w-4 h-4 bg-green-500 rounded-full"></div></div><div className="text-xs font-medium">Ofiste</div></>}
                        {status === 2 && <><div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1"><div className="w-4 h-4 bg-blue-500 rounded-full"></div></div><div className="text-xs font-medium">Uzaktan</div></>}
                        {status === 3 && <><div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1"><div className="w-4 h-4 bg-yellow-500 rounded-full"></div></div><div className="text-xs font-medium">İzinli</div></>}
                        {status === 4 && <><div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1"><div className="w-4 h-4 bg-purple-500 rounded-full"></div></div><div className="text-xs font-medium">Mazeretli</div></>}
                        {status === 5 && <><div className="w-8 h-8 bg-white rounded-full mx-auto flex items-center justify-center mb-1"><div className="w-4 h-4 bg-orange-500 rounded-full"></div></div><div className="text-xs font-medium">Resmi Tatil</div></>}
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="flex-1 p-6 bg-white">
            <h3 className="text-lg font-semibold mb-4">Haftalık Katılım Seçimi</h3>
            <div className="grid grid-cols-5 gap-4 mb-6">
                {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, idx) =>
                    renderDayCard(dayName, idx)
                )}
            </div>

            {officeDays < minDay && (
                <div className="mb-4 p-4 rounded-lg bg-red-50">
                    <p className="text-xs text-red-600">
                        Ofise en az {minDay} gün gelmeniz gerekmektedir. Şu an {officeDays} gün seçili.
                    </p>
                </div>
            )}

            <div className="mb-4 p-4 rounded-lg bg-blue-50">
                <h4 className="text-sm font-medium text-blue-800 mb-2">Durum Açıklamaları:</h4>
                <div className="grid grid-cols-2 gap-2 text-xs text-blue-700">
                    <div>• Gri: Veri yok (tıklayarak seçin)</div>
                    <div>• Yeşil: Ofiste çalışma</div>
                    <div>• Mavi: Uzaktan çalışma</div>
                    <div>• Sarı: İzinli (değiştirilemez)</div>
                    <div>• Mor: Mazeretli</div>
                    <div>• Turuncu: Resmi Tatil</div>
                </div>
            </div>

            <button
                onClick={handleSave}
                className="px-6 py-3 rounded-lg font-medium transition-all flex items-center gap-2 bg-blue-600 text-white hover:bg-blue-700"
            >
                <Save className="w-4 h-4" />
                Kaydet
            </button>

            {/* === DEBUG START === */}
            <div className="mt-6 p-4 border-2 border-red-300 rounded bg-red-50 text-sm text-red-800">
                <strong>DEBUG:</strong><br />
                <div>user: {JSON.stringify(user)}</div>
                <div>weekStart: {weekStart}</div>
                <div>weeklyStatus: {JSON.stringify(weeklyStatus)}</div>
                <div>last POST result: {debugLog}</div>
            </div>
            {/* === DEBUG END === */}
        </div>
    );
};

export default EmployeeAttendanceRegistration;
