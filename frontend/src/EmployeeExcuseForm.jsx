// src/frontend/src/EmployeeExcuseForm.jsx
import React, { useState, useEffect, useMemo } from 'react';
import axios from 'axios';
import { Save, Trash2, Edit3 } from 'lucide-react';
import Swal from 'sweetalert2';
import { useUser } from './UserContext';

const getIstanbulNow = () => {
    const now = new Date();
    const parts = new Intl.DateTimeFormat('en', {
        timeZone: 'Europe/Istanbul',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
    }).formatToParts(now);
    const m = {};
    parts.forEach(p => { if (p.type !== 'literal') m[p.type] = p.value; });
    return new Date(`${m.year}-${m.month}-${m.day}T${m.hour}:${m.minute}:${m.second}`);
};

const TYPES = [
    { val: 0, label: 'Yıllık İzin' },
    { val: 1, label: 'Mazeretli İzin' },
];

export default function EmployeeExcuseForm() {
    const { user } = useUser();
    const [existingExcuses, setExistingExcuses] = useState([]);
    const [selectedDates, setSelectedDates] = useState([]);
    const [attendanceStatus, setAttendanceStatus] = useState(null);
    const [description, setDescription] = useState('');
    const [weeklyAttendanceStatus, setWeeklyAttendanceStatus] = useState([0, 0, 0, 0, 0]);
    const minDay = 1;

    const weekDays = useMemo(() => {
        const today = getIstanbulNow();
        const day = today.getDay();
        const monday = new Date(today);
        monday.setDate(today.getDate() - day + (day === 0 ? -6 : 1));
        monday.setDate(monday.getDate() + 7);
        return Array.from({ length: 5 }, (_, i) => {
            const d = new Date(monday);
            d.setDate(monday.getDate() + i);
            return d;
        });
    }, []);

    const weekDaysStrings = useMemo(
        () => weekDays.map(d => d.toISOString().split('T')[0]),
        [weekDays]
    );
    const weekStart = weekDaysStrings[0];

    useEffect(() => {
        // fetch excuses
        axios.get('/api/excuses', { withCredentials: true })
            .then(res => setExistingExcuses(res.data))
            .catch(console.error);

        // fetch attendance
        axios.get(`/api/attendance?weekStart=${weekStart}`, { withCredentials: true })
            .then(res => {
                if (res.data.length === 5) setWeeklyAttendanceStatus(res.data);
            })
            .catch(console.error);
    }, [weekStart]);

    const existingDates = useMemo(() => {
        return existingExcuses.flatMap(e => {
            const start = new Date(e.startDate);
            const end   = new Date(e.endDate);
            const dates = [];
            for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
                dates.push(d.toISOString().split('T')[0]);
            }
            return dates;
        });
    }, [existingExcuses]);

    const handleDateToggle = (dateStr, idx) => {
        if (
            existingDates.includes(dateStr) ||
            [3,4,5].includes(weeklyAttendanceStatus[idx])
        ) return;
        setSelectedDates(prev =>
            prev.includes(dateStr)
                ? prev.filter(d => d !== dateStr)
                : [...prev, dateStr]
        );
    };

    const handleSubmit = () => {
        if (selectedDates.length < minDay || attendanceStatus === null) {
            return Swal.fire('Eksik bilgi', 'En az 1 gün ve tür seçin', 'warning');
        }

        Swal.fire({
            title: 'Emin misiniz?',
            text: 'Mazeretler gönderilecek ve attendance güncellenecek.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Evet',
            cancelButtonText: 'İptal',
        }).then(({ isConfirmed }) => {
            if (!isConfirmed) return;

            // bundle into one range request
            const start = selectedDates[0];
            const end   = selectedDates[selectedDates.length - 1];
            const payload = {
                startDate:   start,
                endDate:     end,
                reason:      TYPES.find(o => o.val === attendanceStatus).label,
                description
            };

            axios.post('/api/excuses', payload, { withCredentials: true })
                .then(() => {
                    // clear form
                    setSelectedDates([]);
                    setAttendanceStatus(null);
                    setDescription('');
                })
                // reload both lists in one go
                .then(() => Promise.all([
                    axios.get('/api/excuses',          { withCredentials: true }),
                    axios.get(`/api/attendance?weekStart=${weekStart}`, { withCredentials: true })
                ]))
                .then(([excRes, attRes]) => {
                    setExistingExcuses(excRes.data);
                    if (attRes.data.length === 5) setWeeklyAttendanceStatus(attRes.data);
                    Swal.fire('Başarılı', 'Mazeretler kaydedildi', 'success');
                })
                .catch(() => Swal.fire('Hata', 'Kayıt başarısız', 'error'));
        });
    };

    const handleDelete = excuse => {
        Swal.fire({
            title: 'Silinsin mi?',
            text: `${excuse.startDate} tarihli mazeret silinecek.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Evet',
            cancelButtonText: 'İptal'
        }).then(({ isConfirmed }) => {
            if (!isConfirmed) return;
            axios.delete(`/api/excuses/${excuse.id}`, { withCredentials: true })
                .then(() => Promise.all([
                        axios.get('/api/excuses', { withCredentials: true }),
                    axios.get(`/api/attendance?weekStart=${weekStart}`, { withCredentials: true })
                    ]))
                .then(([excRes, attRes]) => {
                        setExistingExcuses(excRes.data);
                        if (attRes.data.length === 5) {
                                setWeeklyAttendanceStatus(attRes.data);
                            }
                        Swal.fire('Silindi', 'Mazeret silindi', 'success');
                    })
                .catch(() => Swal.fire('Hata', 'Silme başarısız', 'error'));
        });
    };

    const handleEdit = excuse => {
        if (excuse.status === 'approved') {
            return Swal.fire('Düzenlenemez', 'Onaylı mazeret düzenlenemez.', 'info');
        }

        Swal.fire({
            title: 'Mazereti Güncelle',
            html:
                `<select id="swal-reason" class="swal2-input">
         <option value="">-- Tür seç --</option>` +
                TYPES.map(o =>
                    `<option value="${o.label}" ${o.label === excuse.reason ? 'selected' : ''}>
           ${o.label}
         </option>`
                ).join('') +
                `</select>` +
                `<textarea id="swal-desc" class="swal2-textarea" placeholder="Açıklama...">${excuse.description || ''}</textarea>`,
            focusConfirm: false,
            showCancelButton: true,
            preConfirm: () => {
                const newReason = document.getElementById('swal-reason').value;
                const newDesc   = document.getElementById('swal-desc').value;
                if (!newReason) Swal.showValidationMessage('Lütfen bir tür seçin');
                return { reason: newReason, description: newDesc };
            }
        }).then(({ isConfirmed, value }) => {
            if (!isConfirmed) return;

            // send PUT to update the existing excuse
            axios.put(`/api/excuses/${excuse.id}`, {
                startDate:   excuse.startDate,
                endDate:     excuse.endDate,
                reason:      value.reason,
                description: value.description
            }, { withCredentials: true })
                // reload lists
                .then(() => Promise.all([
                    axios.get('/api/excuses', { withCredentials: true }),
                    axios.get(`/api/attendance?weekStart=${weekStart}`, { withCredentials: true })
                ]))
                .then(([excRes, attRes]) => {
                    setExistingExcuses(excRes.data);
                    if (attRes.data.length === 5) setWeeklyAttendanceStatus(attRes.data);
                    Swal.fire('Güncellendi', 'Mazeret başarıyla güncellendi', 'success');
                })
                .catch(() => Swal.fire('Hata', 'Güncelleme başarısız', 'error'));
        });
    };

    const formatDate = dateStr => {
        const [y,m,d] = dateStr.split('-');
        return `${d}.${m}`;
    };

    return (
        <div className="p-6 bg-white">
            <h3 className="text-lg font-semibold mb-4">Bu Hafta - Mazeret Günleri</h3>
            <div className="grid grid-cols-5 gap-4 mb-6">
                {weekDaysStrings.map((ds, idx) => {
                    const isExisting = existingDates.includes(ds);
                    const isSelected = selectedDates.includes(ds);
                    const status = weeklyAttendanceStatus[idx];
                    const clickable = !isExisting && ![3,4,5].includes(status);
                    return (
                        <div key={ds} className="text-center">
                            <div className="text-sm text-gray-600 mb-1">{formatDate(ds)}</div>
                            <div
                                onClick={() => clickable && handleDateToggle(ds, idx)}
                                className={`p-4 h-20 flex items-center justify-center rounded-lg border-2 transition
                  ${isExisting ? 'bg-yellow-100 border-yellow-200 text-yellow-700' :
                                    isSelected ? 'bg-purple-500 text-white border-purple-500' :
                                        clickable ? 'bg-white border-gray-200 hover:border-gray-300 hover:shadow cursor-pointer' :
                                            'bg-gray-100 border-gray-200 text-gray-500 cursor-not-allowed'}`
                                }
                            >
                                {isExisting ? <span className="text-xs font-medium">Mevcut</span> :
                                    isSelected ? <span className="text-xs font-medium">Seçili</span> :
                                        <span className="text-xs">Seç</span>}
                            </div>
                        </div>
                    );
                })}
            </div>

            <h3 className="text-lg font-semibold mb-4">Mazeret Türü</h3>
            <div className="flex gap-4 mb-4">
                {TYPES.map(o => (
                    <label key={o.val} className="flex items-center gap-2">
                        <input
                            type="radio"
                            name="type"
                            value={o.val}
                            checked={attendanceStatus === o.val}
                            onChange={() => setAttendanceStatus(o.val)}
                        />
                        <span>{o.label}</span>
                    </label>
                ))}
            </div>

            <textarea
                rows={3}
                value={description}
                onChange={e => setDescription(e.target.value)}
                className="w-full p-3 border rounded-lg mb-4"
                placeholder="Açıklama (isteğe bağlı)"
            />

            <button
                onClick={handleSubmit}
                className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition mb-8"
            >
                <Save className="w-4 h-4 inline-block mr-1" /> Gönder
            </button>

            <h3 className="text-lg font-semibold mb-4">Mevcut Mazeretler</h3>
            {existingExcuses.length === 0 ? (
                <p className="text-gray-500">Henüz mazeretiniz bulunmuyor.</p>
            ) : (
                <div className="space-y-3">
                    {existingExcuses.map(exc => {
                        const rangeLabel = exc.startDate === exc.endDate
                            ? formatDate(exc.startDate)
                            : `${formatDate(exc.startDate)} – ${formatDate(exc.endDate)}`;

                        return (
                            <div key={exc.id} className="p-4 border rounded-lg flex justify-between items-start">
                                <div className="flex-1">
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="font-medium">{rangeLabel}</span>
                                        <span className="text-sm bg-blue-100 text-blue-700 px-2 py-1 rounded">
            {exc.reason}
          </span>
                                    </div>
                                    {exc.description && (
                                        <p className="text-xs text-gray-500">
                                            {exc.description}
                                        </p>
                                    )}
                                </div>
                                <div className="flex gap-2 ml-4">
                                    {exc.status !== 'approved' && (
                                    <button
                                        onClick={() => handleEdit(exc)}
                                        className="text-blue-600 hover:text-blue-800"
                                        title="Düzenle"
                                    >
                                        <Edit3 />
                                    </button>
                                    )}
                                    <button
                                        onClick={() => handleDelete(exc)}
                                        className="text-red-600 hover:text-red-800"
                                        title="Sil"
                                    >
                                        <Trash2 />
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
