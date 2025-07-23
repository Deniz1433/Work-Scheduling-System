// frontend/src/EmployeeExcuseForm.jsx
import React, { useState, useEffect, useMemo } from 'react';
import axios from 'axios';
import { Save, Trash2, Edit3 } from 'lucide-react';
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

const TYPES = [
    { val: 0, label: 'Yıllık İzin' },
    { val: 1, label: 'Mazeretli İzin'}
];

export default function EmployeeExcuseForm() {
    const { user } = useUser();
    const [selectedDates, setSelectedDates] = useState([]);
    const [attendanceStatus, setAttendanceStatus] = useState(0);
    const [description, setDescription] = useState('');
    const [existingExcuses, setExistingExcuses] = useState([]);
    const [weeklyAttendanceStatus, setWeeklyAttendanceStatus] = useState([0, 0, 0, 0, 0]);
    const minDay = 1;

    // Next week's Monday→Friday dates and strings
    const weekDays = useMemo(() => {
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
    }, []);

    const weekDaysStrings = useMemo(() => 
        weekDays.map(d => d.toISOString().split('T')[0]), 
        [weekDays]
    );

    const weekStart = weekDaysStrings[0];

    useEffect(() => {
        // Fetch existing excuses
        const fetchExcuses = axios.get('/api/excuse')
            .then(res => {
                setExistingExcuses(res.data);
                setSelectedDates([]);
            })
            .catch(console.error);

        // Fetch attendance status for the week
        const fetchAttendance = axios.get(`/api/attendance?weekStart=${weekStart}`)
            .then(res => {
                if (res.data && res.data.length === 5) {
                    setWeeklyAttendanceStatus(res.data);
                }
            })
            .catch(err => console.error('Fetch attendance failed', err));

        Promise.all([fetchExcuses, fetchAttendance]);
    }, [weekStart]);

    const dayNames = ['Pazartesi','Salı','Çarşamba','Perşembe','Cuma'];
    const existingDates = useMemo(() => existingExcuses.map(e => e.excuseDate), [existingExcuses]);

    // Sort excuses by date for better display
    const sortedExcuses = useMemo(() => {
        return [...existingExcuses].sort((a, b) => new Date(a.excuseDate) - new Date(b.excuseDate));
    }, [existingExcuses]);

    const handleDateToggle = (dateStr, dayIndex) => {
        // Eğer bu tarih için zaten excuse varsa veya attendance durumu İzinli/Resmi Tatil/Mazeretli ise tıklanamaz
        if (existingDates.includes(dateStr) || 
            weeklyAttendanceStatus[dayIndex] === 3 || 
            weeklyAttendanceStatus[dayIndex] === 4 || 
            weeklyAttendanceStatus[dayIndex] === 5) {
            return;
        }
        
        setSelectedDates(prev =>
            prev.includes(dateStr) ? prev.filter(d => d !== dateStr) : [...prev, dateStr]
        );
    };

    const handleSubmit = () => {
        if (selectedDates.length < minDay || attendanceStatus === '' || attendanceStatus === undefined) {
            return Swal.fire('Eksik bilgi','En az bir gün ve tür seçin','warning');
        }
        
        Swal.fire({
            title: 'Emin misiniz?',
            text: 'Mazeretler gönderilecek ve attendance durumunuz güncellenecek.',
            icon: 'warning',
            showCancelButton: true,
            cancelButtonText: 'İptal',
            confirmButtonText: 'Evet',
        }).then(r => {
            if (!r.isConfirmed) return;
            
            // Her seçili tarih için excuse oluştur
            const excusePromises = selectedDates.map(d =>
                axios.post('/api/excuse', {
                    dates: [d],
                    excuseType: attendanceStatus,
                    description
                })
            );

            // Attendance durumunu güncelle
            const newWeeklyStatus = [...weeklyAttendanceStatus];
            selectedDates.forEach(dateStr => {
                const dayIndex = weekDaysStrings.indexOf(dateStr);
                if (dayIndex !== -1) {
                    // Mazeret türüne göre attendance durumunu belirle
                    // 0: Yıllık İzin -> attendance status 3 (İzinli)
                    // 1: Mazeretli İzin -> attendance status 4 (Mazeretli)
                    newWeeklyStatus[dayIndex] = attendanceStatus === 0 ? 3 : 4;
                }
            });

            const attendancePromise = axios.post('/api/attendance', {
                userId: user.id,
                weekStart: weekStart,
                dates: newWeeklyStatus
            });

            Promise.all([...excusePromises, attendancePromise])
                .then(() => {
                    // Verileri yeniden yükle
                    return Promise.all([
                        axios.get('/api/excuse'),
                        axios.get(`/api/attendance?weekStart=${weekStart}`)
                    ]);
                })
                .then(([excuseRes, attendanceRes]) => {
                    setExistingExcuses(excuseRes.data);
                    if (attendanceRes.data && attendanceRes.data.length === 5) {
                        setWeeklyAttendanceStatus(attendanceRes.data);
                    }
                    setSelectedDates([]);
                    setAttendanceStatus('');
                    setDescription('');
                    Swal.fire('Başarılı','Mazeretler kaydedildi ve attendance güncellendi','success');
                })
                .catch(() => Swal.fire('Hata','Kayıt başarısız','error'));
        });
    };

    const handleDeleteSingle = excuse => {
        const formattedDate = new Date(excuse.excuseDate).toLocaleDateString('tr-TR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
        
        Swal.fire({
            title: 'Silinsin mi?',
            text: `${formattedDate} tarihli mazeret silinecek ve attendance durumu güncellenecek.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Evet',
            cancelButtonText: 'İptal'
        }).then(r => {
            if (!r.isConfirmed) return;
            
            // Excuse'u sil
            const deletePromise = axios.delete(`/api/excuse/${excuse.id}`);
            
            // Attendance durumunu güncelle
            const excuseDate = excuse.excuseDate;
            const dayIndex = weekDaysStrings.indexOf(excuseDate);
            let attendancePromise = Promise.resolve();
            
            if (dayIndex !== -1) {
                const newWeeklyStatus = [...weeklyAttendanceStatus];
                newWeeklyStatus[dayIndex] = 0; // Veri yok durumuna çevir
                
                attendancePromise = axios.post('/api/attendance', {
                    userId: user.id,
                    weekStart: weekStart,
                    dates: newWeeklyStatus
                });
            }
            
            Promise.all([deletePromise, attendancePromise])
                .then(() => {
                    return Promise.all([
                        axios.get('/api/excuse'),
                        axios.get(`/api/attendance?weekStart=${weekStart}`)
                    ]);
                })
                .then(([excuseRes, attendanceRes]) => {
                    setExistingExcuses(excuseRes.data);
                    if (attendanceRes.data && attendanceRes.data.length === 5) {
                        setWeeklyAttendanceStatus(attendanceRes.data);
                    }
                    Swal.fire('Silindi','Mazeret silindi ve attendance güncellendi','success');
                })
                .catch(() => Swal.fire('Hata','Silme başarısız','error'));
        });
    };

    const handleEditSingle = excuse => {
        const openDialog = () => {
            Swal.fire({
                title: 'Mazereti Güncelle',
                html:
                    `<select id="swal-type" class="swal2-input">
                        <option value="">-- Tür seç --</option>` +
                    TYPES.map(o =>
                        `<option value="${o.val}" ${o.val===excuse.excuseType?'selected':''}>${o.label}</option>`
                    ).join('') +
                    `</select>` +
                    `<textarea id="swal-desc" class="swal2-textarea" placeholder="Açıklama...">${excuse.description}</textarea>`,
                focusConfirm: false,
                preConfirm: () => {
                    const t = document.getElementById('swal-type').value;
                    const d = document.getElementById('swal-desc').value;
                    if (!t) Swal.showValidationMessage('Tür seçmelisiniz');
                    return { excuseType: parseInt(t), description: d };
                }
            }).then(r => {
                if (!r.isConfirmed) return;
                
                // Excuse'u güncelle
                const updatePromise = axios.post(`/api/excuse/${excuse.id}`,{
                    id: excuse.id,
                    excuseType: document.getElementById('swal-type').value,
                    description: document.getElementById('swal-desc').value
                });
                
                // Attendance durumunu güncelle
                const excuseDate = excuse.excuseDate;
                const dayIndex = weekDaysStrings.indexOf(excuseDate);
                let attendancePromise = Promise.resolve();
                
                if (dayIndex !== -1) {
                    const newWeeklyStatus = [...weeklyAttendanceStatus];
                    // Yeni mazeret türüne göre attendance durumunu güncelle
                    newWeeklyStatus[dayIndex] = r.value.excuseType === 0 ? 3 : 4;
                    
                    attendancePromise = axios.post('/api/attendance', {
                        userId: user.id,
                        weekStart: weekStart,
                        dates: newWeeklyStatus
                    });
                }
                
                Promise.all([updatePromise, attendancePromise])
                    .then(() => {
                        return Promise.all([
                            axios.get('/api/excuse'),
                            axios.get(`/api/attendance?weekStart=${weekStart}`)
                        ]);
                    })
                    .then(([excuseRes, attendanceRes]) => {
                        setExistingExcuses(excuseRes.data);
                        if (attendanceRes.data && attendanceRes.data.length === 5) {
                            setWeeklyAttendanceStatus(attendanceRes.data);
                        }
                        Swal.fire('Güncellendi','Mazeret güncellendi ve onay kaldırıldı.','success');
                    })
                    .catch(() => Swal.fire('Hata','Güncelleme başarısız','error'));
            });
        };

        if (excuse.isApproved) {
            Swal.fire({
                title: 'Onay kaldırılacak',
                text: 'Düzenlemek onayını kaldıracak. Devam edilsin mi?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Evet',
                cancelButtonText: 'İptal'
            }).then(r => r.isConfirmed && openDialog());
        } else {
            openDialog();
        }
    };

    const formatExcuseDate = (dateStr) => {
        const date = new Date(dateStr);
        const dayName = dayNames[date.getDay() - 1];
        const formattedDate = date.toLocaleDateString('tr-TR', {
            day: '2-digit',
            month: '2-digit'
        });
        return `${dayName} (${formattedDate})`;
    };

    const getStatusLabel = (attendanceStatus) => {
        switch(attendanceStatus) {
            case 0: return 'Veri Yok';
            case 1: return 'Ofiste';
            case 2: return 'Uzaktan';
            case 3: return 'İzinli';
            case 4: return 'Mazeretli';
            case 5: return 'Resmi Tatil';
            default: return 'Bilinmeyen';
        }
    };

    const getStatusStyle = (attendanceStatus) => {
        switch(attendanceStatus) {
            case 0: return 'bg-gray-100 border-gray-200 text-gray-500';
            case 1: return 'bg-green-100 text-green-700 border-green-200';
            case 2: return 'bg-blue-100 text-blue-700 border-blue-200';
            case 3: return 'bg-yellow-100 text-yellow-700 border-yellow-200';
            case 4: return 'bg-purple-100 text-purple-700 border-purple-200';
            case 5: return 'bg-orange-100 text-orange-700 border-orange-200';
            default: return 'bg-gray-100 border-gray-200 text-gray-500';
        }
    };

    return (
        <div className="p-6 bg-white">
            <h3 className="text-lg font-semibold mb-4">Bu Hafta - Mazeret Günleri</h3>

            {/* Calendar */}
            <div className="grid grid-cols-5 gap-4 mb-6">
                {dayNames.map((day, i) => {
                    const ds = weekDaysStrings[i];
                    const attendanceStatusForDay = weeklyAttendanceStatus[i];
                    const hasExistingExcuse = existingDates.includes(ds);
                    const isSelected = selectedDates.includes(ds);
                    const isClickable = !hasExistingExcuse && 
                                       attendanceStatusForDay !== 3 && 
                                       attendanceStatusForDay !== 4 && 
                                       attendanceStatusForDay !== 5;

                    return (
                        <div key={ds} className="text-center">
                            <div className="text-sm font-medium text-gray-600 mb-2">{day}</div>
                            <div className="text-xs text-gray-500 mb-3">
                                {new Date(ds).toLocaleDateString('tr-TR',{day:'2-digit',month:'2-digit'})}
                            </div>
                            
                            {hasExistingExcuse ? (
                                <div className="p-4 h-20 flex items-center justify-center rounded-lg border-2 bg-green-100 text-green-700 border-green-200">
                                    <span className="text-xs font-medium">İzinli</span>
                                </div>
                            ) : (
                                <div
                                    onClick={() => isClickable && handleDateToggle(ds, i)}
                                    className={`
                                        p-4 h-20 flex items-center justify-center rounded-lg border-2 transition
                                        ${isSelected
                                            ? 'bg-orange-500 text-white border-orange-500 shadow-lg'
                                            : isClickable
                                                ? 'bg-white border-gray-200 hover:border-gray-300 hover:shadow-md cursor-pointer'
                                                : `${getStatusStyle(attendanceStatusForDay)} cursor-not-allowed`
                                        }
                                    `}
                                >
                                    {isSelected ? (
                                        <span className="text-xs font-medium">Seçili</span>
                                    ) : isClickable ? (
                                        <span className="text-xs text-gray-500">Seç</span>
                                    ) : (
                                        <span className="text-xs font-medium">
                                            {getStatusLabel(attendanceStatusForDay)}
                                        </span>
                                    )}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {/* New-excuse form */}
            <div className="mb-6">
                <h3 className="text-lg font-semibold mb-4">Mazeret Türü</h3>
                <div className="flex flex-col gap-3">
                    {TYPES.map(opt => (
                        <label key={opt.val} className="flex items-center gap-3 cursor-pointer">
                            <input
                                type="radio"
                                value={opt.val}
                                checked={attendanceStatus===opt.val}
                                onChange={e=>setAttendanceStatus(Number(e.target.value))}
                                className="w-4 h-4"
                            />
                            <span>{opt.label}</span>
                        </label>
                    ))}
                </div>
            </div>
            
            <div className="mb-8">
                <textarea
                    rows={4}
                    value={description}
                    onChange={e=>setDescription(e.target.value)}
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 resize-none"
                    placeholder="Açıklama..."
                />
            </div>
            
            <button
                onClick={handleSubmit}
                className="mb-8 px-6 py-3 bg-green-600 text-white rounded-lg flex items-center gap-2 hover:bg-green-700 transition"
            >
                <Save className="w-4 h-4"/> Gönder
            </button>

            {/* Mevcut Mazeretler */}
            <h3 className="text-lg font-semibold mb-4">Mevcut Mazeretler</h3>
            {sortedExcuses.length === 0 && (
                <p className="text-sm text-gray-500">Henüz mazeretiniz bulunmuyor.</p>
            )}
            <div className="space-y-3">
                {sortedExcuses.map(excuse => (
                    <div key={excuse.id} className="p-4 border rounded-lg bg-gray-50">
                        <div className="flex justify-between items-start">
                            <div className="flex-1">
                                <div className="flex items-center gap-3 mb-2">
                                    <span className="font-medium text-gray-900">
                                        {formatExcuseDate(excuse.excuseDate)}
                                    </span>
                                    <span className="text-sm bg-blue-100 text-blue-700 px-2 py-1 rounded">
                                        {TYPES.find(o=>o.val===excuse.excuseType)?.label}
                                    </span>
                                    {excuse.isApproved && (
                                        <span className="text-sm bg-green-100 text-green-700 px-2 py-1 rounded">
                                            Onaylı
                                        </span>
                                    )}
                                </div>
                                {excuse.description && (
                                    <p className="text-sm text-gray-600 mt-1">
                                        {excuse.description}
                                    </p>
                                )}
                            </div>
                            <div className="flex gap-2 ml-4">
                                <button 
                                    onClick={() => handleEditSingle(excuse)} 
                                    className="flex items-center gap-1 text-blue-600 hover:text-blue-800 text-sm px-2 py-1 rounded hover:bg-blue-50 transition"
                                    title="Düzenle"
                                >
                                    <Edit3 size={14}/> 
                                </button>
                                <button 
                                    onClick={() => handleDeleteSingle(excuse)} 
                                    className="flex items-center gap-1 text-red-600 hover:text-red-800 text-sm px-2 py-1 rounded hover:bg-red-50 transition"
                                    title="Sil"
                                >
                                    <Trash2 size={14}/> 
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}