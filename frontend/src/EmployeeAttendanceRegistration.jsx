import React, { useState, useEffect } from 'react';
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

const EXCUSE_TYPES = [
    { val: 0, label: 'Yıllık İzin' },
    { val: 1, label: 'Mazeretli İzin'}
];

const EmployeeAttendanceRegistration = () => {  
    const {user} = useUser();

    // 5 elemanlı array: [monday, tuesday, wednesday, thursday, friday]
    // Her eleman 0-5 arası durum kodunu tutar
    const [weeklyStatus, setWeeklyStatus] = useState([0, 0, 0, 0, 0]);
    const [existingExcuses, setExistingExcuses] = useState([]);
    // Yeni excuse'lar için açıklama sakla (dayIndex -> {excuseType, description})
    const [pendingExcuseDescriptions, setPendingExcuseDescriptions] = useState({});
    const [loading, setLoading] = useState(true);
    const [isAttendanceApproved, setIsAttendanceApproved] = useState(false);
    const [originalWeeklyStatus, setOriginalWeeklyStatus] = useState([0, 0, 0, 0, 0]);
    // Get minDay from user's department, fallback to 2
    const [minDay, setMinDay] = useState(2);

    // Update minDay when user data changes
    useEffect(() => {
        if (user?.department?.minDays !== undefined) {
            setMinDay(user.department.minDays);
        }
    }, [user]);

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
    
    const weekStart = weekDays[0].toISOString().split('T')[0];
    const weekDaysStrings = weekDays.map(d => d.toISOString().split('T')[0]);

    // Seçenekler - Resmi tatil (5) hariç, kullanıcı seçebilir
    const statusOptions = [
        { value: 0, label: 'Seçiniz', color: 'text-gray-500' },
        { value: 1, label: 'Ofiste', color: 'text-green-600' },
        { value: 2, label: 'Uzaktan', color: 'text-blue-600' },
        { value: 3, label: 'İzinli', color: 'text-yellow-600' },
        { value: 4, label: 'Mazeretli', color: 'text-purple-600' }
    ];

    const statusStyles = {
        0: { bg: 'bg-gray-100', border: 'border-gray-200', text: 'text-gray-500' },
        1: { bg: 'bg-green-100', border: 'border-green-300', text: 'text-green-700' },
        2: { bg: 'bg-blue-100', border: 'border-blue-300', text: 'text-blue-700' },
        3: { bg: 'bg-yellow-100', border: 'border-yellow-300', text: 'text-yellow-700' },
        4: { bg: 'bg-purple-100', border: 'border-purple-300', text: 'text-purple-700' },
        5: { bg: 'bg-orange-100', border: 'border-orange-300', text: 'text-orange-700' }
    };

    useEffect(() => {
        // Check if user is available
        if (!user || !user.keycloakId) {
            console.log('User not available yet:', user);
            return;
        }

        // Fetch attendance data
        const fetchData = async () => {
            try {
                // First get user ID
                const userIdResponse = await axios.get(`/api/userInfo/${user.keycloakId}`);
                const userId = userIdResponse.data;
                console.log('User ID:', userId);
                
                if (!userId) {
                    console.error('User ID not found for keycloakId:', user.keycloakId);
                    throw new Error('Kullanıcı ID bulunamadı');
                }

                // Then fetch attendance data
                const attendanceResponse = await axios.get(`/api/attendance/${weekStart}`);
                const attendanceData = attendanceResponse.data;
                
                if (attendanceData && attendanceData.length === 2) {
                    setWeeklyStatus(attendanceData[0]);
                    setOriginalWeeklyStatus(attendanceData[0]);
                }
                console.log('Attendance data:', attendanceData);
                
                // Check if attendance is approved
                if (attendanceData && attendanceData[1]) {
                    setIsAttendanceApproved(true);
                }

                // Fetch existing excuses
                const excusesResponse = await axios.get('/api/excuse');
                setExistingExcuses(excusesResponse.data);
            } catch (error) {
                console.error('Error fetching data:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [weekStart, user]);

    const handleStatusChange = async (dayIndex, newStatus) => {
        const oldStatus = weeklyStatus[dayIndex];
        const newStatusInt = parseInt(newStatus);
        const excuseDate = weekDaysStrings[dayIndex];
        
        // aynı gün için zaten excuse var mı kontrol et
        const existingExcuseForDay = existingExcuses.find(e => e.excuseDate === excuseDate);
        
        // İzinli (3) veya Mazeretli (4) seçildiğinde
        if (newStatusInt === 3 || newStatusInt === 4) {
            // Eğer bu gün için zaten excuse varsa izin verme
            if (existingExcuseForDay) {
                Swal.fire({
                    title: 'Hata',
                    text: 'Bu gün için zaten bir mazeret mevcut. Önce mevcut mazereti silmeniz gerekiyor.',
                    icon: 'error',
                    confirmButtonText: 'Tamam'
                });
                return; // Status değişikliği yapmadan çık
            }

            const excuseType = newStatusInt === 3 ? 0 : 1; // 3->0 (Yıllık), 4->1 (Mazeretli)
            const excuseTypeLabel = EXCUSE_TYPES.find(t => t.val === excuseType)?.label;
            
            const result = await Swal.fire({
                title: `${excuseTypeLabel} - Açıklama Gerekli`,
                html: `
                    <div class="text-left mb-4">
                        <label class="block text-sm font-medium text-gray-700 mb-2">
                            Açıklama <span class="text-red-500">*</span>
                        </label>
                        <textarea 
                            id="excuse-description" 
                            rows="4" 
                            class="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 resize-none" 
                            placeholder="Açıklama giriniz..."
                        ></textarea>
                    </div>
                `,
                showCancelButton: true,
                confirmButtonText: 'Tamam',
                cancelButtonText: 'İptal',
                preConfirm: () => {
                    const description = document.getElementById('excuse-description').value.trim();
                    if (!description) {
                        Swal.showValidationMessage('Açıklama zorunludur!');
                        return false;
                    }
                    return description;
                }
            });

            if (result.isConfirmed) {
                // Sadece state'i güncelle, backend'e kaydetme
                setWeeklyStatus(prev => {
                    const newStatusArray = [...prev];
                    newStatusArray[dayIndex] = newStatusInt;
                    return newStatusArray;
                });

                // Pending excuse description'ı sakla
                setPendingExcuseDescriptions(prev => ({
                    ...prev,
                    [dayIndex]: {
                        excuseType: excuseType,
                        description: result.value
                    }
                }));
            } else {
                // İptal edildi, status değişikliği yapma
                return;
            }
        } else {
            // Normal durum değişikliği (İzinli/Mazeretli değil)
            
            // Eğer önceki durum İzinli/Mazeretli idi, pending description'ı temizle
            if (oldStatus === 3 || oldStatus === 4) {
                setPendingExcuseDescriptions(prev => {
                    const newPending = { ...prev };
                    delete newPending[dayIndex];
                    return newPending;
                });
            }

            setWeeklyStatus(prev => {
                const newStatusArray = [...prev];
                newStatusArray[dayIndex] = newStatusInt;
                return newStatusArray;
            });
        }
    };

    // Ofiste olacak gün sayısını hesapla (status = 1)
    const officeDays = weeklyStatus.filter(status => status === 1).length;

    const handleSave = async () => {
        if(weeklyStatus.includes(0)){
            return Swal.fire('Eksik bilgi','Lütfen tüm günleri doldurunuz.','warning');
        }
        
        // Check if user is available
        if (!user || !user.keycloakId) {
            return Swal.fire('Hata', 'Kullanıcı bilgileri yüklenemedi. Lütfen sayfayı yenileyin.', 'error');
        }
        
        // Check if attendance was approved and has been modified
        const hasChanges = JSON.stringify(weeklyStatus) !== JSON.stringify(originalWeeklyStatus);
        const needsApprovalWarning = isAttendanceApproved && hasChanges;
        
        let warningText = '';
        if (needsApprovalWarning) {
            warningText = 'Onaylı attendance kaydınız değiştirildi. Kaydetmek onayınızı kaldıracaktır. Devam etmek istiyor musunuz?';
        } else if (officeDays < minDay) {
            warningText = `Ofise en az ${minDay} gün gelmeniz gerekmektedir. Yine de kaydetmek istiyor musunuz?`;
        } else {
            warningText = 'Seçiminiz kaydedilecektir.';
        }

        const result = await Swal.fire({
            title: 'Emin misiniz?',
            text: warningText,
            icon: needsApprovalWarning ? 'warning' : 'warning',
            showCancelButton: true,
            confirmButtonText: 'Evet',
            cancelButtonText: 'Hayır',
        });

        if (result.isConfirmed) {
            try {
                // Önce yeni excuse'ları oluştur
                const excusePromises = [];
                Object.keys(pendingExcuseDescriptions).forEach(dayIndex => {
                    const excuseData = pendingExcuseDescriptions[dayIndex];
                    const excuseDate = weekDaysStrings[dayIndex];
                    
                    excusePromises.push(
                        axios.post('/api/excuse', {
                            dates: [excuseDate],
                            excuseType: excuseData.excuseType,
                            description: excuseData.description
                        })
                    );
                });

                if (excusePromises.length > 0) {
                    await Promise.all(excusePromises);
                }

                // Get user ID first
                const userIdResponse = await axios.get(`/api/userInfo/${user.keycloakId}`);
                const userId = userIdResponse.data;
                
                if (!userId) {
                    throw new Error('Kullanıcı ID bulunamadı');
                }

                // Sonra attendance'ı kaydet - isApproved false olacak
                await axios.post('/api/attendance', { 
                    userId: userId, // Long ID
                    weekStart: weekStart,
                    dates: weeklyStatus
                });

                // Excuse listesini yenile
                const excuseRes = await axios.get('/api/excuse');
                setExistingExcuses(excuseRes.data);

                // Pending descriptions'ı temizle
                setPendingExcuseDescriptions({});
                
                // Update approval status
                setIsAttendanceApproved(false);
                setOriginalWeeklyStatus([...weeklyStatus]);

                Swal.fire('Başarılı', 'Seçiminiz kaydedildi.', 'success');
            } catch (error) {
                console.error('Kaydetme hatası:', error);
                console.error('Error response:', error.response);
                console.error('Error message:', error.message);
                Swal.fire('Hata', 'Kayıt sırasında bir sorun oluştu: ' + (error.response?.data?.error || error.message), 'error');
            }
        }
    };

    const handleDeleteExcuse = async (excuse) => {
        const formattedDate = new Date(excuse.excuseDate).toLocaleDateString('tr-TR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
        
        const result = await Swal.fire({
            title: 'Silinsin mi?',
            text: `${formattedDate} tarihli mazeret silinecek ve attendance durumu güncellenecek.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Evet',
            cancelButtonText: 'İptal'
        });

        if (result.isConfirmed) {
            try {
                // Kendi excuse'ını sil
                await axios.delete(`/api/excuse/my/${excuse.id}`);
                
                // Attendance durumunu güncelle
                const excuseDate = excuse.excuseDate;
                const dayIndex = weekDaysStrings.indexOf(excuseDate);
                if (dayIndex !== -1) {
                    setWeeklyStatus(prev => {
                        const newStatusArray = [...prev];
                        newStatusArray[dayIndex] = 1; // Veri yok durumuna çevir
                        
                        // State güncellemesi sonrası API çağrısı yap
                        setTimeout(async () => {
                            try {
                                // Get user ID first
                                const userIdResponse = await axios.get(`/api/userInfo/${user.keycloakId}`);
                                const userId = userIdResponse.data;
                                
                                if (!userId) {
                                    throw new Error('Kullanıcı ID bulunamadı');
                                }
                                
                                await axios.post('/api/attendance', { 
                                    userId: userId, // Long ID
                                    weekStart: weekStart,
                                    dates: newStatusArray
                                });
                            } catch (error) {
                                console.error('Attendance güncelleme hatası:', error);
                            }
                        }, 0);
                        
                        return newStatusArray;
                    });
                    
                    // Pending description varsa temizle
                    setPendingExcuseDescriptions(prev => {
                        const newPending = { ...prev };
                        delete newPending[dayIndex];
                        return newPending;
                    });
                }
                
                // Excuse listesini yenile
                const excuseRes = await axios.get('/api/excuse');
                setExistingExcuses(excuseRes.data);
                
                

                Swal.fire('Silindi','Mazeret silindi ve attendance güncellendi','success');
            } catch (error) {
                console.error('Excuse silme hatası:', error);
                Swal.fire('Hata','Silme başarısız','error');
            }
        }
    };

    const handleEditExcuse = async (excuse) => {
        const openDialog = async () => {
            const result = await Swal.fire({
                title: 'Mazereti Güncelle',
                html:
                    `<div class="text-left">
                        <label class="block text-sm font-medium text-gray-700 mb-2">Mazeret Türü</label>
                        <select id="swal-type" class="w-full p-2 border border-gray-300 rounded-lg mb-4">
                            <option value="">-- Tür seç --</option>` +
                    EXCUSE_TYPES.map(o =>
                        `<option value="${o.val}" ${o.val===excuse.excuseType?'selected':''}>${o.label}</option>`
                    ).join('') +
                    `</select>
                    <label class="block text-sm font-medium text-gray-700 mb-2">Açıklama *</label>
                    <textarea id="swal-desc" class="w-full p-3 border border-gray-300 rounded-lg resize-none" rows="4" placeholder="Açıklama...">${excuse.description}</textarea>
                    </div>`,
                focusConfirm: false,
                showCancelButton: true,
                confirmButtonText: 'Güncelle',
                cancelButtonText: 'İptal',
                preConfirm: () => {
                    const t = document.getElementById('swal-type').value;
                    const d = document.getElementById('swal-desc').value.trim();
                    if (!t) {
                        Swal.showValidationMessage('Tür seçmelisiniz');
                        return false;
                    }
                    if (!d) {
                        Swal.showValidationMessage('Açıklama zorunludur');
                        return false;
                    }
                    return { excuseType: parseInt(t), description: d };
                }
            });

            if (result.isConfirmed) {
                try {
                    // Excuse'u güncelle
                    await axios.post(`/api/excuse/${excuse.id}`, {
                        id: excuse.id,
                        excuseType: result.value.excuseType,
                        description: result.value.description
                    });
                    
                    // Attendance durumunu güncelle
                    const excuseDate = excuse.excuseDate;
                    const dayIndex = weekDaysStrings.indexOf(excuseDate);
                    
                    if (dayIndex !== -1) {
                        setWeeklyStatus(prev => {
                            const newStatusArray = [...prev];
                            // Yeni mazeret türüne göre attendance durumunu güncelle
                            newStatusArray[dayIndex] = result.value.excuseType === 0 ? 3 : 4;
                            
                            // State güncellemesi sonrası API çağrısı yap
                            setTimeout(async () => {
                                try {
                                    // Get user ID first
                                    const userIdResponse = await axios.get(`/api/userInfo/${user.keycloakId}`);
                                    const userId = userIdResponse.data;
                                    
                                    if (!userId) {
                                        throw new Error('Kullanıcı ID bulunamadı');
                                    }
                                    
                                    await axios.post('/api/attendance', { 
                                        userId: userId, // Long ID
                                        weekStart: weekStart,
                                        dates: newStatusArray
                                    });
                                } catch (error) {
                                    console.error('Attendance güncelleme hatası:', error);
                                }
                            }, 0);
                            
                            return newStatusArray;
                        });
                    }
                    
                    // Excuse listesini yenile
                    const excuseRes = await axios.get('/api/excuse');
                    setExistingExcuses(excuseRes.data);
                    
                    Swal.fire('Güncellendi','Mazeret güncellendi','success');
                } catch (error) {
                    console.error('Excuse güncelleme hatası:', error);
                    Swal.fire('Hata','Güncelleme başarısız','error');
                }
            }
        };

        if (excuse.isApproved) {
            const result = await Swal.fire({
                title: 'Onay kaldırılacak',
                text: 'Düzenlemek onayını kaldıracak. Devam edilsin mi?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Evet',
                cancelButtonText: 'İptal'
            });
            
            if (result.isConfirmed) {
                openDialog();
            }
        } else {
            openDialog();
        }
    };

    const renderDayCard = (dayName, dayIndex) => {
        const date = weekDays[dayIndex];
        const status = weeklyStatus[dayIndex];
        const style = statusStyles[status];
        const isResmiTatil = status === 5; // Resmi tatil değiştirilemez
        const excuseDate = weekDaysStrings[dayIndex];
        const hasExistingExcuse = existingExcuses.find(e => e.excuseDate === excuseDate);
        const hasPendingDescription = pendingExcuseDescriptions[dayIndex];

        return (
            <div key={dayIndex} className="text-center">
                <div className="text-sm font-medium text-gray-700 mb-2">{dayName}</div>
                <div className="text-xs text-gray-500 mb-3">
                    {date.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
                </div>
                
                <div className={`p-4 rounded-lg border-2 ${style.bg} ${style.border} min-h-[120px] relative`}>
                    {isResmiTatil ? (
                        // Resmi tatil - değiştirilemez
                        <div className="flex flex-col items-center justify-center h-full">
                            <div className="w-8 h-8 bg-orange-500 rounded-full mx-auto flex items-center justify-center mb-2">
                                <div className="w-4 h-4 bg-white rounded-full"></div>
                            </div>
                            <div className="text-sm font-medium text-orange-700">Resmi Tatil</div>
                            <div className="text-xs text-orange-600 mt-1">(Değiştirilemez)</div>
                        </div>
                    ) : (
                        // Normal günler - combobox
                        <div className="flex flex-col items-center justify-center h-full">
                            <select
                                value={status}
                                onChange={(e) => handleStatusChange(dayIndex, e.target.value)}
                                className={`
                                    w-full px-3 py-2 rounded-md border border-gray-300 
                                    focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                                    text-sm font-medium bg-white
                                    ${status === 0 ? 'text-gray-500' : statusOptions.find(opt => opt.value === status)?.color || 'text-gray-700'}
                                `}
                                disabled={hasExistingExcuse} // Zaten excuse varsa disable et
                            >
                                {statusOptions.map(option => (
                                    <option 
                                        key={option.value} 
                                        value={option.value}
                                        className={option.color}
                                    >
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                            
                            {/* Seçilen duruma göre görsel gösterge */}
                            {status > 0 && (
                                <div className="mt-2 flex items-center justify-center">
                                    <div className={`w-3 h-3 rounded-full ${
                                        status === 1 ? 'bg-green-500' :
                                        status === 2 ? 'bg-blue-500' :
                                        status === 3 ? 'bg-yellow-500' :
                                        status === 4 ? 'bg-purple-500' : 'bg-gray-400'
                                    }`}></div>
                                </div>
                            )}

                            {/* Existing excuse varsa gösterge */}
                            {hasExistingExcuse && (
                                <div className="absolute top-1 right-1">
                                    <div className="w-2 h-2 bg-green-500 rounded-full" title="Kayıtlı mazeret var"></div>
                                </div>
                            )}

                            {/* Pending description varsa gösterge */}
                            {hasPendingDescription && !hasExistingExcuse && (
                                <div>
                                    <div className="absolute top-1 right-1">
                                        <div className="w-2 h-2 bg-orange-500 rounded-full" title="Kaydedilmeyi bekliyor"></div>
                                    </div>
                                    <div className="text-sm text-gray-600 mt-2 break-words overflow-hidden">
                                        {hasPendingDescription.description}
                                    </div>
                                </div>
                                
                            )}
                        </div>
                    )}
                </div>
            </div>
        );
    };

    // Bu haftanın excuse'larını filtrele
    const thisWeekExcuses = existingExcuses.filter(excuse => 
        weekDaysStrings.includes(excuse.excuseDate)
    ).sort((a, b) => new Date(a.excuseDate) - new Date(b.excuseDate));

    const formatExcuseDate = (dateStr) => {
        const date = new Date(dateStr);
        const dayNames = ['Pazartesi','Salı','Çarşamba','Perşembe','Cuma'];
        const dayName = dayNames[date.getDay() - 1];
        const formattedDate = date.toLocaleDateString('tr-TR', {
            day: '2-digit',
            month: '2-digit'
        });
        return `${dayName} (${formattedDate})`;
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-lg">Yükleniyor...</div>
            </div>
        );
    }

    return (
        <div className="flex-1 p-6 bg-white">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Bu Hafta - İş Günleri</h3>
                {isAttendanceApproved && (
                    <div className="flex items-center gap-2 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
                        <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                        Onaylı
                    </div>
                )}
            </div>

            <div className="grid grid-cols-5 gap-4 mb-6">
                {['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma'].map((dayName, idx) => 
                    renderDayCard(dayName, idx)
                )}
            </div>

            {officeDays < minDay && !isAttendanceApproved && (
                <div className="mb-4 p-4 rounded-lg bg-red-50 border border-red-200">
                    <p className="text-sm text-red-700">
                        ⚠️ Ofise en az {minDay} gün gelmeniz gerekmektedir. Şu an {officeDays} gün seçili.
                    </p>
                </div>
            )}

            {/* Onaylı attendance değiştirildiğinde uyarı göster */}
            {isAttendanceApproved && JSON.stringify(weeklyStatus) !== JSON.stringify(originalWeeklyStatus) && (
                <div className="mb-4 p-4 rounded-lg bg-orange-50 border border-orange-200">
                    <p className="text-sm text-orange-700">
                        ⚠️ Onaylı attendance kaydınız değiştirildi. Kaydetmek onayınızı kaldıracaktır.
                    </p>
                </div>
            )}

            {/* Pending excuse'lar varsa uyarı göster */}
            {Object.keys(pendingExcuseDescriptions).length > 0 && (
                <div className="mb-4 p-4 rounded-lg bg-yellow-50 border border-yellow-200">
                    <p className="text-sm text-yellow-700">
                        ⏳ {Object.keys(pendingExcuseDescriptions).length} adet mazeret açıklaması kaydedilmeyi bekliyor. 
                        Kaydetmek için "Kaydet" butonunu kullanın.
                    </p>
                </div>
            )}

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
                <div className="mt-3 pt-3 border-t border-blue-200">
                    <div className="text-xs text-blue-600">
                        <strong>Göstergeler:</strong> 
                        <span className="inline-flex items-center gap-1 ml-2">
                            <div className="w-2 h-2 bg-green-500 rounded-full"></div> Kayıtlı mazeret
                        </span>
                        <span className="inline-flex items-center gap-1 ml-3">
                            <div className="w-2 h-2 bg-orange-500 rounded-full"></div> Kaydedilmeyi bekliyor
                        </span>
                    </div>
                </div>
            </div>

            {/* Bu haftanın excuse'ları */}
            {thisWeekExcuses.length > 0 && (
                <div className="mb-6">
                    <h4 className="text-lg font-semibold mb-3">Bu Haftanın Mazeretleri</h4>
                    <div className="space-y-3">
                        {thisWeekExcuses.map(excuse => (
                            <div key={excuse.id} className="p-4 border rounded-lg bg-gray-50">
                                <div className="flex justify-between items-start">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-2">
                                            <span className="font-medium text-gray-900">
                                                {formatExcuseDate(excuse.excuseDate)}
                                            </span>
                                            <span className="text-sm bg-blue-100 text-blue-700 px-2 py-1 rounded">
                                                {EXCUSE_TYPES.find(o=>o.val===excuse.excuseType)?.label}
                                            </span>
                                            {excuse.approved || excuse.isApproved && (
                                                <span className="text-sm bg-green-100 text-green-700 px-2 py-1 rounded">
                                                    Onaylı
                                                </span>
                                            )}
                                            {excuse.approved === false || excuse.isApproved === false && (
                                                <span className="text-sm bg-red-100 text-red-700 px-2 py-1 rounded">
                                                    Onaylı Değil
                                                </span>
                                            )}
                                        </div>
                                        {excuse.description && (
                                            <p className="text-sm text-gray-600 mt-1 break-words overflow-hidden">
                                                {excuse.description}
                                            </p>
                                        )}
                                    </div>
                                    <div className="flex gap-2 ml-4">
                                        <button 
                                            onClick={() => handleEditExcuse(excuse)} 
                                            className="flex items-center gap-1 text-blue-600 hover:text-blue-800 text-sm px-2 py-1 rounded hover:bg-blue-50 transition"
                                            title="Düzenle"
                                        >
                                            <Edit3 size={14}/> 
                                        </button>
                                        <button 
                                            onClick={() => handleDeleteExcuse(excuse)} 
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
            )}

            <button
                onClick={handleSave}
                className="px-6 py-3 rounded-lg font-medium transition-all flex items-center gap-2 bg-blue-600 text-white hover:bg-blue-700 disabled:bg-gray-400"
                disabled={weeklyStatus.includes(0)}
            >
                <Save className="w-4 h-4" />
                Kaydet
            </button>
        </div>
    );
};

export default EmployeeAttendanceRegistration;