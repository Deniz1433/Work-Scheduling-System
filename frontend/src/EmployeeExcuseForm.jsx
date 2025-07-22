// frontend/src/EmployeeExcuseForm.jsx
import React, { useState, useEffect, useMemo } from 'react';
import axios from 'axios';
import { Save, Trash2, Edit3 } from 'lucide-react';
import Swal from 'sweetalert2';

const TYPES = [
  { val: 'yillik-izin', label: 'Yıllık İzin' },
  { val: 'mazeret',    label: 'Mazeret'    },
  { val: 'gelmicem',   label: 'Gelmicem'   },
];

export default function EmployeeExcuseForm() {
  const [selectedDates, setSelectedDates] = useState([]);
  const [attendanceStatus, setAttendanceStatus] = useState('');
  const [description, setDescription] = useState('');
  const [existingExcuses, setExistingExcuses] = useState([]);
  const minDay = 1;

  useEffect(() => {
    axios.get('/api/excuse')
        .then(res => {
          setExistingExcuses(res.data);
          setSelectedDates([]);
        })
        .catch(console.error);
  }, []);

  const weekDays = useMemo(() => {
    const today = new Date(), day = today.getDay();
    const mon = new Date(today.setDate(today.getDate() - day + (day === 0 ? -6 : 1)));
    return Array.from({ length: 5 }, (_, i) => {
      const d = new Date(mon);
      d.setDate(mon.getDate() + i);
      return d.toISOString().split('T')[0];
    });
  }, []);
  const dayNames = ['Pazartesi','Salı','Çarşamba','Perşembe','Cuma'];
  const existingDates = useMemo(() => existingExcuses.map(e => e.excuseDate), [existingExcuses]);

  // group by type+desc
  const grouped = useMemo(() => {
    const map = {};
    existingExcuses.forEach(e => {
      const key = `${e.excuseType}:::${e.description}`;
      if (!map[key]) {
        map[key] = {
          ids: [],
          dates: [],
          excuseType: e.excuseType,
          description: e.description,
          isApproved: e.isApproved
        };
      }
      map[key].ids.push(e.id);
      map[key].dates.push(e.excuseDate);
      // if any row is unapproved, mark group unapproved
      if (!e.isApproved) map[key].isApproved = false;
    });
    return Object.values(map).map(g => ({
      ...g,
      dates: g.dates.sort()
    }));
  }, [existingExcuses]);

  const handleDateToggle = dateStr => {
    if (existingDates.includes(dateStr)) return;
    setSelectedDates(prev =>
        prev.includes(dateStr)
            ? prev.filter(d => d !== dateStr)
            : [...prev, dateStr]
    );
  };

  const handleSubmit = () => {
    if (selectedDates.length < minDay || !attendanceStatus) {
      return Swal.fire('Eksik bilgi','En az bir gün ve tür seçin','warning');
    }
    Swal.fire({
      title: 'Emin misiniz?',
      text: 'Mazeretler gönderilecek.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Evet',
    }).then(r => {
      if (!r.isConfirmed) return;
      Promise.all(selectedDates.map(d =>
          axios.post('/api/excuse', {
            dates: [d],
            excuseType: attendanceStatus,
            description
          })
      ))
          .then(() => axios.get('/api/excuse'))
          .then(res => {
            setExistingExcuses(res.data);
            setSelectedDates([]);
            setAttendanceStatus('');
            setDescription('');
            Swal.fire('Başarılı','Mazeretler kaydedildi','success');
          })
          .catch(() => Swal.fire('Hata','Kayıt başarısız','error'));
    });
  };

  const handleDeleteGroup = group => {
    Swal.fire({
      title: 'Silinsin mi?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Evet',
    }).then(r => {
      if (!r.isConfirmed) return;
      Promise.all(group.ids.map(id => axios.delete(`/api/excuse/${id}`)))
          .then(() => axios.get('/api/excuse'))
          .then(res => {
            setExistingExcuses(res.data);
            Swal.fire('Silindi','Mazeret silindi','success');
          })
          .catch(() => Swal.fire('Hata','Silme başarısız','error'));
    });
  };

  const handleEditGroup = group => {
    const openDialog = () => {
      Swal.fire({
        title: 'Mazereti Güncelle',
        html:
            `<select id="swal-type" class="swal2-input">
             <option value="">-- Tür seç --</option>` +
            TYPES.map(o =>
                `<option value="${o.val}" ${o.val===group.excuseType?'selected':''}>${o.label}</option>`
            ).join('') +
            `</select>` +
            `<textarea id="swal-desc" class="swal2-textarea">${group.description}</textarea>`,
        focusConfirm: false,
        preConfirm: () => {
          const t = document.getElementById('swal-type').value;
          const d = document.getElementById('swal-desc').value;
          if (!t) Swal.showValidationMessage('Tür seçmelisiniz');
          return { excuseType: t, description: d };
        }
      }).then(r => {
        if (!r.isConfirmed) return;
        Promise.all(group.ids.map(id =>
            axios.put(`/api/excuse/${id}`, r.value)
        ))
            .then(() => axios.get('/api/excuse'))
            .then(res => {
              setExistingExcuses(res.data);
              Swal.fire('Güncellendi','Mazeret güncellendi ve onay kaldırıldı.','success');
            })
            .catch(() => Swal.fire('Hata','Güncelleme başarısız','error'));
      });
    };

    if (group.isApproved) {
      Swal.fire({
        title: 'Onay kaldırılacak',
        text: 'Düzenlemek onayını kaldıracak. Devam edilsin mi?',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Evet',
      }).then(r => r.isConfirmed && openDialog());
    } else {
      openDialog();
    }
  };

  return (
      <div className="p-6 bg-white">
        <h3 className="text-lg font-semibold mb-4">Bu Hafta - Mazeret Günleri</h3>

        {/* Calendar */}
        <div className="grid grid-cols-5 gap-4 mb-6">
          {dayNames.map((day, i) => {
            const ds = weekDays[i];
            if (existingDates.includes(ds)) {
              return (
                  <div key={ds} className="text-center">
                    <div className="text-sm font-medium text-gray-600 mb-2">{day}</div>
                    <div className="text-xs text-gray-500 mb-3">
                      {new Date(ds).toLocaleDateString('tr-TR',{day:'2-digit',month:'2-digit'})}
                    </div>
                    <div className="p-4 h-20 flex items-center justify-center rounded-lg border-2 bg-green-100 text-green-700">
                      <span className="text-xs font-medium">İzinli</span>
                    </div>
                  </div>
              );
            }
            const sel = selectedDates.includes(ds);
            return (
                <div key={ds} className="text-center">
                  <div className="text-sm font-medium text-gray-600 mb-2">{day}</div>
                  <div className="text-xs text-gray-500 mb-3">
                    {new Date(ds).toLocaleDateString('tr-TR',{day:'2-digit',month:'2-digit'})}
                  </div>
                  <div
                      onClick={()=>handleDateToggle(ds)}
                      className={`
                  p-4 h-20 flex items-center justify-center cursor-pointer rounded-lg border-2 transition
                  ${sel
                          ? 'bg-orange-500 text-white border-orange-500 shadow-lg'
                          : 'bg-white border-gray-200 hover:border-gray-300 hover:shadow-md'
                      }
                `}
                  >
                    {sel
                        ? <span className="text-xs font-medium">Seçili</span>
                        : <span className="text-xs text-gray-500">Seç</span>
                    }
                  </div>
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
                      onChange={e=>setAttendanceStatus(e.target.value)}
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
        {grouped.length === 0 && (
            <p className="text-sm text-gray-500">Henüz mazeretiniz bulunmuyor.</p>
        )}
        <div className="space-y-4">
          {grouped.map(group => (
              <div key={group.dates.join(',')} className="p-4 border rounded-lg flex justify-between items-start">
                <div>
                  <p><strong>{group.dates.join(', ')}</strong> — {TYPES.find(o=>o.val===group.excuseType)?.label}</p>
                  <p className="text-sm mt-1">{group.description}</p>
                  {group.isApproved && (
                      <span className="inline-block mt-2 bg-green-100 text-green-700 text-xs px-2 py-1 rounded">
                  Onaylı
                </span>
                  )}
                </div>
                <div className="flex flex-col gap-2">
                  <button onClick={()=>handleEditGroup(group)} className="flex items-center gap-1 text-blue-600 hover:text-blue-800">
                    <Edit3 size={16}/> Düzenle
                  </button>
                  <button onClick={()=>handleDeleteGroup(group)} className="flex items-center gap-1 text-red-600 hover:text-red-800">
                    <Trash2 size={16}/> Sil
                  </button>
                </div>
              </div>
          ))}
        </div>
      </div>
  );
}
