import React, {useEffect, useState} from 'react';
import axios from 'axios';
import * as XLSX from 'xlsx';
import Select from 'react-select';
import Swal from 'sweetalert2';

const EmployeeTeamAttendance = ({ user }) => {

  const [teamState, setTeamState] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Filtreleme state'leri
  const [, setDepartments] = useState([]);
  const [roles, setRoles] = useState([]);
  const [selectedDepartments, setSelectedDepartments] = useState([]);
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedWorkStatus, setSelectedWorkStatus] = useState([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [currentWeekOffset, setCurrentWeekOffset] = useState(1); // Hafta offset'i (0 = bu hafta, 1 = gelecek hafta, -1 = geçen hafta)
  
  // Çalışma durumu seçenekleri
  const workStatusOptions = [
    { value: '1', label: 'Ofiste' },
    { value: '2', label: 'Uzaktan' },
    { value: '3', label: 'İzinli' },
    { value: '4', label: 'Mazeretli' }
  ];
  
  const [userPermissions, setUserPermissions] = useState(null);
  const [editPermissions, setEditPermissions] = useState(null);
  const [memberEditPermissions, setMemberEditPermissions] = useState({});
  const [filteredDepartments, setFilteredDepartments] = useState([]);
  const [, setFilteredRoles] = useState([]);

  // Kullanıcı yetkilerini yükle
  const fetchUserPermissions = async () => {
    try {
      const [permissionsResponse, editPermissionsResponse] = await Promise.all([
        fetch('/api/attendance/user-permissions'),
        fetch('/api/attendance/edit-permissions')
      ]);
      
      const permissions = await permissionsResponse.json();
      const editPerms = await editPermissionsResponse.json();
      
      setUserPermissions(permissions);
      setEditPermissions(editPerms);
      console.log('🔍 User permissions:', permissions);
      console.log('🔍 Edit permissions:', editPerms);
      console.log('🔍 canViewDepartment:', permissions.canViewDepartment);
      console.log('🔍 userDepartmentId:', permissions.userDepartmentId);
    } catch (err) {
      console.error('Kullanıcı yetkileri yüklenemedi:', err);
    }
  };

  // Departman ve rol verilerini yükle (yetkilere göre filtrelenmiş)
  const fetchFilterData = async () => {
    try {
      const [rolesResponse] = await Promise.all([
        fetch('/api/roles')
      ]);
      
      const rolesData = await rolesResponse.json();
      
      // Rolleri kontrol et
      if (!Array.isArray(rolesData)) {
        console.warn('Roller için beklenmeyen veri formatı alındı:', rolesData);
        setRoles([]);
        setFilteredRoles([]);
      } else {
        setRoles(rolesData);
        setFilteredRoles(rolesData);
      }
      
      // Yetkilere göre departmanları filtrele
      let filteredDepts = [];
      if (userPermissions) {
        if (userPermissions.canViewAll) {
          // Tüm departmanları göster
          const departmentsResponse = await fetch('/api/departments');
          const departmentsData = await departmentsResponse.json();
          if (Array.isArray(departmentsData)) {
            filteredDepts = departmentsData;
          } else {
            console.warn('Departmanlar için beklenmeyen veri formatı alındı:', departmentsData);
            filteredDepts = [];
          }
        } else if (userPermissions.canViewChild) {
          // Child departmanları göster
          const childDepartmentsResponse = await fetch('/api/departments/child-departments');
          const childDepartmentsData = await childDepartmentsResponse.json();
          if (Array.isArray(childDepartmentsData)) {
            filteredDepts = childDepartmentsData;
          } else {
            console.warn('Child departmanlar için beklenmeyen veri formatı alındı:', childDepartmentsData);
            filteredDepts = [];
          }
        } else if (userPermissions.canViewDepartment) {
          // Sadece kendi departmanını göster
          const departmentsResponse = await fetch('/api/departments');
          const departmentsData = await departmentsResponse.json();
          if (Array.isArray(departmentsData)) {
            filteredDepts = departmentsData.filter(dept => 
              dept.id === userPermissions.userDepartmentId
            );
          } else {
            console.warn('Departmanlar için beklenmeyen veri formatı alındı:', departmentsData);
            filteredDepts = [];
          }
        } else {
          // Hiçbir yetki yoksa boş liste
          console.warn('Hiçbir departman görüntüleme yetkisi yok!');
          filteredDepts = [];
        }
      }
      
      setDepartments(filteredDepts);
      setFilteredDepartments(filteredDepts);
    } catch (err) {
      console.error('Filtre verileri yüklenemedi:', err);
      setRoles([]);
      setFilteredRoles([]);
      setDepartments([]);
      setFilteredDepartments([]);
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
        console.log('🔍 Adding department filter:', deptIds);
      }
      if (selectedRoles.length > 0) {
        const roleIds = selectedRoles.map(role => role.value).join(',');
        params.append('roleId', roleIds);
        console.log('🔍 Adding role filter:', roleIds);
      }
      if (searchTerm) {
        params.append('searchTerm', searchTerm);
        console.log('🔍 Adding search filter:', searchTerm);
      }
      if (selectedWorkStatus.length > 0) {
        const workStatusIds = selectedWorkStatus.map(status => status.value).join(',');
        params.append('workStatus', workStatusIds);
        console.log('🔍 Adding workStatus filter:', workStatusIds);
        console.log('🔍 Selected work status options:', selectedWorkStatus);
      }
      if (startDate) {
        params.append('startDate', startDate);
        console.log('🔍 Adding startDate filter:', startDate);
      }
      if (endDate) {
        params.append('endDate', endDate);
        console.log('🔍 Adding endDate filter:', endDate);
      }
      
      // Hafta başlangıç tarihini de gönder
      params.append('weekStart', weekStart);
      console.log('🔍 Adding weekStart parameter:', weekStart);

      console.log('🔍 Fetching team data with params:', params.toString());
      const response = await fetch(`/api/attendance/team?${params.toString()}`);
      const data = await response.json();
      console.log('🔍 Backend response:', data);
      console.log('🔍 Response status:', response.status);
      
      // Veri formatını kontrol et
      if (!Array.isArray(data)) {
        console.warn('Backend\'den beklenmeyen veri formatı alındı:', data);
        setTeamState([]);
        setMemberEditPermissions({});
        return;
      }
      
      console.log('🔍 Setting team state with', data.length, 'users');
      setTeamState(data);
      
      // Takım üyelerinin edit yetkilerini kontrol et
      if (data.length > 0) {
        const userIds = data.map(member => member.id);
        const editPermissionsResponse = await fetch('/api/attendance/check-edit-permissions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(userIds)
        });
        
        if (editPermissionsResponse.ok) {
          const permissions = await editPermissionsResponse.json();
          setMemberEditPermissions(permissions);
          console.log('Member edit permissions:', permissions);
        }
      } else {
        // Boş array durumunda edit permissions'ları temizle
        setMemberEditPermissions({});
      }
    }
    catch (err) {
      console.error('Takım verileri alınırken hata:', err);
      setError('Veri alınamadı.');
      setTeamState([]);
      setMemberEditPermissions({});
    } finally {
      setLoading(false);
    }
  };

  // Hafta navigasyon fonksiyonları
  const goToPreviousWeek = () => {
    setCurrentWeekOffset(prev => prev - 1);
  };

  const goToNextWeek = () => {
    setCurrentWeekOffset(prev => prev + 1);
  };

  const goToCurrentWeek = () => {
    setCurrentWeekOffset(0);
  };

  // Filtreleri temizle
  const clearFilters = () => {
    setSelectedDepartments([]);
    setSelectedRoles([]);
    setSearchTerm('');
    setSelectedWorkStatus([]);
    setStartDate('');
    setEndDate('');
    setCurrentWeekOffset(0); // Hafta offset'i de sıfırla
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
        return {
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
      });

             // Hafta bilgisi
       const weekInfo = {
         'Hafta Başlangıcı': weekDays[0].toLocaleDateString('tr-TR'),
         'Hafta Bitişi': weekDays[4].toLocaleDateString('tr-TR'),
         'Hafta Offset': currentWeekOffset === 0 ? 'Bu Hafta' : 
                        currentWeekOffset > 0 ? `${currentWeekOffset} Hafta Sonra` : 
                        `${Math.abs(currentWeekOffset)} Hafta Önce`,
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
         'Arama Terimi': searchTerm || 'Yok',
         'Çalışma Durumu Filtresi': selectedWorkStatus.length > 0 
           ? selectedWorkStatus.map(s => s.label).join(', ') 
           : 'Tüm Durumlar',
         'Başlangıç Tarihi': startDate || 'Belirtilmedi',
         'Bitiş Tarihi': endDate || 'Belirtilmedi'
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
         { 'Bilgi': 'Hafta Konumu', 'Değer': weekInfo['Hafta Offset'] },
         { 'Bilgi': 'Export Tarihi', 'Değer': weekInfo['Export Tarihi'] },
         { 'Bilgi': 'Toplam Kişi', 'Değer': weekInfo['Toplam Kişi'] },
         { 'Bilgi': '', 'Değer': '' },
         { 'Bilgi': 'Filtre Bilgileri', 'Değer': '' },
         { 'Bilgi': 'Departman Filtresi', 'Değer': filterInfo['Departman Filtresi'] },
         { 'Bilgi': 'Rol Filtresi', 'Değer': filterInfo['Rol Filtresi'] },
         { 'Bilgi': 'Arama Terimi', 'Değer': filterInfo['Arama Terimi'] },
         { 'Bilgi': 'Çalışma Durumu Filtresi', 'Değer': filterInfo['Çalışma Durumu Filtresi'] }
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

  // Component mount olduğunda kullanıcı yetkilerini yükle, sonra filtre verilerini yükle
  useEffect(() => {
    const initializeData = async () => {
      await fetchUserPermissions();
    };
    initializeData();
  }, []);

  // Kullanıcı yetkileri yüklendiğinde filtre verilerini yükle
  useEffect(() => {
    if (userPermissions) {
      fetchFilterData();
    }
  }, [userPermissions]);

  // Hafta değiştiğinde verileri yeniden çek
  useEffect(() => {
    if (userPermissions && teamState.length > 0) {
      fetchTeamData();
    }
  }, [currentWeekOffset]);

  // Add this useEffect after your existing useEffects
  useEffect(() => {
    // Auto-fetch data when component mounts and userPermissions are available
    if (userPermissions) {
      fetchTeamData();
    }
  }, [userPermissions]);

  // helper: normalize id types
  const toNum = (v) => (v == null ? null : Number(v));

  // helper: is this row the current user?
  const isSelf = (member) => toNum(user?.id) === toNum(member?.id);

  // helper: do I have any "edit others" ability?
  const canEditOthers =
      !!(editPermissions?.canEditAll || editPermissions?.canEditChild || editPermissions?.canEditDepartment);


  const canEditMember = (member) => {
    // Team view: self is only allowed if the user can edit others
    if (isSelf(member)) return canEditOthers;

    // If backend supplied a per-member answer, trust it
    const key = String(member?.id);
    if (memberEditPermissions[key] !== undefined) {
      return memberEditPermissions[key];
    }

    // Default conservative: no
    return false;
  };




  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingMember, setEditingMember] = useState(null);
  const [tempAttendance, setTempAttendance] = useState([]);
  const [editReason, setEditReason] = useState('');
  const [employeeExcuses, setEmployeeExcuses] = useState([]);
  const [isEditLoading, setIsEditLoading] = useState(false);

  const generateWeekDays = (weekOffset = 0) => {
    const today = new Date();
    const day = today.getDay();
    const currentMonday = new Date(today);
    currentMonday.setDate(today.getDate() - day + (day === 0 ? -6 : 1));
    
    // Offset'e göre haftayı hesapla
    const targetMonday = new Date(currentMonday);
    targetMonday.setDate(currentMonday.getDate() + (weekOffset * 7));
    
    const weekDays = [];
    for (let i = 0; i < 5; i++) {
      const date = new Date(targetMonday);
      date.setDate(targetMonday.getDate() + i);
      weekDays.push(date);
    }
    return weekDays;
  };

  const weekDays = generateWeekDays(currentWeekOffset);
  const weekStart = weekDays[0].toISOString().split('T')[0];

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
  // Approve button
  const handleApprove = async (memberId) => {
    try {
      // Authz check you already have:
      const member = teamState.find(m => m.id === memberId);
      if (!member || !canEditMember(member)) {
        Swal.fire({
          title: 'Permission Error',
          text: 'You do not have permission to approve this user’s attendance.',
          icon: 'error'
        });
        return;
      }

      // 1) Always fetch the user's excuses fresh here
      const excuseRes = await fetch(`/api/excuse/user/${memberId}`);
      const allExcuses = await excuseRes.json(); // [{ id, userId, excuseDate, excuseType, description, isApproved }, ...]

      // 2) Filter to THIS WEEK and only those not yet approved
      const start = new Date(`${weekStart}T00:00:00`);        // weekStart is your YYYY-MM-DD
      const end = new Date(start);
      end.setDate(start.getDate() + 6);

      const inWeek = (isoDateStr) => {
        const d = new Date(`${isoDateStr}T00:00:00`);
        return d >= start && d <= end;
      };

      const pendingThisWeek = allExcuses.filter(ex =>
          inWeek(ex.excuseDate) && !(ex.isApproved === true || ex.approved === true) // handle both shapes
      );

      // 3) Approve attendance
      await axios.post(`/api/attendance/${memberId}/${weekStart}/approve`);

      // 4) Approve all pending excuses for the same week (in parallel)
      if (pendingThisWeek.length > 0) {
        await Promise.all(
            pendingThisWeek.map(ex =>
                axios.post(`/api/attendance/excuse/${ex.id}/approve`)
            )
        );
      }

      // 5) Refresh UI
      await fetchTeamData();

    } catch (error) {
      console.error('API Error:', error);
      if (error.response?.status === 403) {
        Swal.fire({
          title: 'Permission Error',
          text: 'You do not have permission to approve this user’s attendance/excuses.',
          icon: 'error'
        });
      } else {
        Swal.fire({
          title: 'Error',
          text: 'There was an error while approving: ' + (error.response?.data?.message || error.message),
          icon: 'error'
        });
      }
    }
  };


  // Düzenleme fonksiyonu - Modal açma
  const handleEdit = async (memberId) => {
    if (isEditLoading) return; // Eğer zaten loading durumundaysa, işlemi engelle
    
    // Yetki kontrolü yap
    const member = teamState.find(m => m.id === memberId);
    if (!member || !canEditMember(member)) {
      Swal.fire({
        title: 'Yetki Hatası',
        text: 'Bu kullanıcının attendance bilgisini düzenleme yetkiniz yok.',
        icon: 'error'
      });
      return;
    }
    
    setIsEditLoading(true);
    try {
      setEditingMember(member);
      setTempAttendance([...member.attendance]);
      const excuseInfo = await fetch(`/api/excuse/user/${memberId}`);
      const excuseData = await excuseInfo.json();
      setEmployeeExcuses(excuseData);
      console.log(excuseData);
      setEditReason(''); // Düzenleme sebebi başlangıçta boş
      setIsEditModalOpen(true);
    } catch (error) {
      console.error('Edit modal açılırken hata:', error);
      Swal.fire({
        title: 'Hata',
        text: 'Düzenleme modalı açılırken hata oluştu!',
        icon: 'error'
      });
    } finally {
      setIsEditLoading(false);
    }
  };

  // Modal'daki değişiklikleri kaydetme
  const handleSaveChanges = async () => {
    if (isEditLoading) return;

    setIsEditLoading(true);
    try {
      const localYMD = (d) => {
        const dt = new Date(d);
        const y = dt.getFullYear();
        const m = String(dt.getMonth() + 1).padStart(2, '0');
        const da = String(dt.getDate()).padStart(2, '0');
        return `${y}-${m}-${da}`; // LOCAL date, no UTC conversion
      };

      const payload = {
        userId: editingMember.id.toString(),
        weekStart: localYMD(weekDays[0]),
        dates: tempAttendance,
        explanation: editReason
      };
      console.log('Sending request with data:', payload);

      // 1) Attendance update + email
      const response = await axios.post(`/api/attendance/${editingMember.id}`, payload);

      // 2) Delete excuses ONLY for days that changed from excused(3/4) -> non-excused(1/2)
      try {
        if (Array.isArray(employeeExcuses) && employeeExcuses.length > 0) {
          // Build direct map by the string date exactly as backend returns (YYYY-MM-DD)
          const excusesByExactDate = new Map(
              employeeExcuses.map(e => [e.excuseDate, e]) // assume 'YYYY-MM-DD'
          );

          const plusDays = (dateStr, delta) => {
            const [y, m, d] = dateStr.split('-').map(Number);
            const dt = new Date(y, m - 1, d);
            dt.setDate(dt.getDate() + delta);
            return localYMD(dt);
          };

          const deletions = [];
          const daysToCheck = Math.min(
              weekDays.length,
              tempAttendance.length,
              editingMember.attendance.length
          );

          for (let i = 0; i < daysToCheck; i++) {
            const wasExcused = editingMember.attendance[i] === 3 || editingMember.attendance[i] === 4;
            const nowNotExcused = tempAttendance[i] === 1 || tempAttendance[i] === 2;

            if (!wasExcused || !nowNotExcused) continue;

            const dayKey = localYMD(weekDays[i]);

            // Try exact match first
            let excuse = excusesByExactDate.get(dayKey);

            // Fallbacks to handle any server/client TZ drift if needed
            if (!excuse) excuse = excusesByExactDate.get(plusDays(dayKey, +1));
            if (!excuse) excuse = excusesByExactDate.get(plusDays(dayKey, -1));

            // Final fallback: linear search by tolerant matching
            if (!excuse) {
              excuse = employeeExcuses.find(e =>
                  e.excuseDate === dayKey ||
                  e.excuseDate === plusDays(dayKey, +1) ||
                  e.excuseDate === plusDays(dayKey, -1)
              );
            }

            if (excuse && excuse.id != null) {
              console.log('Deleting excuse for', dayKey, 'id:', excuse.id, 'type:', excuse.excuseType);
              deletions.push(axios.delete(`/api/excuse/${excuse.id}`));
            } else {
              console.warn('No matching excuse found for', dayKey, '— skipping delete.');
            }
          }

          if (deletions.length > 0) {
            await Promise.all(deletions);
          }
        }
      } catch (delErr) {
        console.error('Excuse delete step failed:', delErr);
        // Optional: show a non-blocking toast; do not fail the whole save
      }

      console.log('API Response:', response);

      // 3) Refresh UI
      await fetchTeamData();

      // 4) Reset modal state
      setIsEditModalOpen(false);
      setEditingMember(null);
      setTempAttendance([]);
      setEditReason('');

      Swal.fire({
        title: 'Başarılı',
        text: 'Değişiklikler kaydedildi ve e-posta gönderildi!',
        icon: 'success'
      });

    } catch (error) {
      console.error('API Error:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      Swal.fire({
        title: 'Hata',
        text: 'Değişiklikler kaydedilirken hata oluştu: ' + (error.response?.data?.message || error.message),
        icon: 'error'
      });
    } finally {
      setIsEditLoading(false);
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

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4 mb-4">
                     {/* Departman Filtresi */}
           <div>
             <label className="block text-sm font-medium text-gray-700 mb-2">
               Departman
             </label>
             {userPermissions && (
               <Select
                 isMulti
                 value={selectedDepartments}
                 onChange={setSelectedDepartments}
                 options={filteredDepartments.map(dept => ({ value: dept.id, label: dept.name }))}
                 placeholder={
                   userPermissions.canViewAll ? "Tüm departmanlar" :
                   userPermissions.canViewChild ? "Alt departmanlar" :
                   userPermissions.canViewDepartment ? "Kendi departmanınız" :
                   "Yetkiniz yok"
                 }
                 className="text-sm"
                 classNamePrefix="select"
                 noOptionsMessage={() => "Departman bulunamadı"}
                 isClearable={true}
                 isSearchable={true}
                 isDisabled={!userPermissions.canViewAll && !userPermissions.canViewChild && !userPermissions.canViewDepartment}
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
             )}
             {!userPermissions && (
               <div className="text-sm text-gray-500">Yetkiler yükleniyor...</div>
             )}
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
                   color: 'text-white'
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

          {/* Çalışma Durumu Filtresi */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Çalışma Durumu
            </label>
            <Select
              isMulti
              value={selectedWorkStatus}
              onChange={setSelectedWorkStatus}
              options={workStatusOptions}
              placeholder="Durum seçin..."
              className="text-sm"
              classNamePrefix="select"
              noOptionsMessage={() => "Durum bulunamadı"}
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

          {/* Başlangıç Tarihi Filtresi */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Başlangıç Tarihi
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Başlangıç tarihi seçin..."
            />
          </div>

          {/* Bitiş Tarihi Filtresi */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Bitiş Tarihi
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Bitiş tarihi seçin..."
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
            Ara
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
                      <div key={excuse.id} className="border-b last:border-b-0 py-2">
                        <p className="text-gray-800 text-sm">{excuse.excuseDate}</p>
                        <p className="text-gray-800 text-sm">{excuse.description}</p>
                        <span className="inline-block text-xs mt-1 px-2 py-0.5 rounded bg-gray-200">
                          {excuse.excuseType === 0 ? "Yıllık İzin" : "Mazeretli İzin"}
                        </span>
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
                    <div className="space-y-3">
                      {tempAttendance[index] === 5 ? (
                        // Tatil günü - seçenekleri göster (superadmin için)
                        <div className="space-y-2">
                          <div className="text-center mb-2">
                            <div className="w-4 h-4 bg-orange-500 rounded-full mx-auto mb-1"></div>
                            <span className="text-xs text-orange-700 font-medium">Resmi Tatil</span>
                          </div>
                          {[
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
                          ))}
                        </div>
                      ) : (
                        // Normal gün - seçenekleri göster
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
                        ))
                      )}
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
                  disabled={!editReason.trim() || isEditLoading}
                  className={`px-4 py-2 rounded transition-colors ${
                    !editReason.trim() || isEditLoading
                      ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                      : 'bg-blue-500 text-white hover:bg-blue-600'
                  }`}
                >
                  {isEditLoading ? 'Kaydediliyor...' : 'Değişiklikleri Kaydet'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

             <div className="mb-6">
         {/* Hafta Navigasyonu */}
         <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mb-4 p-4 bg-blue-50 rounded-lg border border-blue-200 shadow-sm">
           <button
             onClick={goToPreviousWeek}
             disabled={currentWeekOffset <= -8} // En fazla 8 hafta geriye git
             className={`px-2 sm:px-3 py-2 text-xs sm:text-sm rounded-lg transition-colors flex items-center gap-1 sm:gap-2 ${
               currentWeekOffset <= -8
                 ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                 : 'bg-blue-500 text-white hover:bg-blue-600'
             }`}
             title={currentWeekOffset <= -8 ? 'En fazla 8 hafta geriye gidebilirsiniz' : 'Önceki haftaya git'}
           >
             <span>←</span>
             <span className="hidden sm:inline">Önceki Hafta</span>
             <span className="sm:hidden">Önceki</span>
           </button>
           
           <button
             onClick={goToCurrentWeek}
             className={`px-4 py-2 text-sm rounded-lg transition-colors ${
               currentWeekOffset === 0 
                 ? 'bg-green-500 text-white' 
                 : 'bg-gray-500 text-white hover:bg-gray-600'
             }`}
           >
             Bu Hafta
           </button>
           
           <button
             onClick={goToNextWeek}
             disabled={currentWeekOffset >= 4} // En fazla 4 hafta ileriye git
             className={`px-2 sm:px-3 py-2 text-xs sm:text-sm rounded-lg transition-colors flex items-center gap-1 sm:gap-2 ${
               currentWeekOffset >= 4
                 ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                 : 'bg-blue-500 text-white hover:bg-blue-600'
             }`}
             title={currentWeekOffset >= 4 ? 'En fazla 4 hafta ileriye gidebilirsiniz' : 'Sonraki haftaya git'}
           >
             <span className="hidden sm:inline">Sonraki Hafta</span>
             <span className="sm:hidden">Sonraki</span>
             <span>→</span>
           </button>
           
           <div className="text-sm text-gray-600 font-medium text-center">
             <div className="mb-1">
               {weekDays[0].toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })} - {weekDays[4].toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' })}
             </div>
             {currentWeekOffset !== 0 && (
               <span className={`px-2 py-1 rounded text-xs ${
                 currentWeekOffset > 0 
                   ? 'bg-green-100 text-green-700' 
                   : 'bg-orange-100 text-orange-700'
               }`}>
                 {currentWeekOffset > 0 ? `+${currentWeekOffset} hafta` : `${currentWeekOffset} hafta`}
               </span>
             )}
           </div>
         </div>

         <div className="flex justify-between items-center mb-4">
                      <h2 className="text-xl">
              {userPermissions ? (
                selectedDepartments.length > 0 || selectedRoles.length > 0 || searchTerm.trim() || selectedWorkStatus.length > 0 || startDate || endDate
                  ? 'Filtrelenmiş Takım Verileri'
                  : userPermissions.canViewAll ? 'Tüm Takım Verileri' :
                    userPermissions.canViewChild ? 'Alt Departman Takım Verileri' :
                    userPermissions.canViewDepartment ? 'Departman Takım Verileri' :
                    'Yetkiniz Yok'
              ) : 'Yükleniyor...'
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
                       {userPermissions && (
              <div className="flex items-center gap-4 text-sm">
                <span className="text-gray-600">Görüntüleme Yetkileri:</span>
                {userPermissions.canViewAll && (
                  <span className="px-2 py-1 bg-green-100 text-green-800 rounded">Tüm Görüntüleme</span>
                )}
                {userPermissions.canViewChild && (
                  <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded">Alt Departman Görüntüleme</span>
                )}
                {userPermissions.canViewDepartment && (
                  <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded">Departman Görüntüleme</span>
                )}
                {!userPermissions.canViewAll && !userPermissions.canViewChild && !userPermissions.canViewDepartment && (
                  <span className="px-2 py-1 bg-red-100 text-red-800 rounded">Görüntüleme Yetkisi Yok</span>
                )}
              </div>
            )}
            {editPermissions && (
              <div className="flex items-center gap-4 text-sm">
                <span className="text-gray-600">Düzenleme Yetkileri:</span>
                {editPermissions.canEditAll && (
                  <span className="px-2 py-1 bg-green-100 text-green-800 rounded">Tüm Düzenleme</span>
                )}
                {editPermissions.canEditChild && (
                  <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded">Alt Departman Düzenleme</span>
                )}
                {editPermissions.canEditDepartment && (
                  <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded">Departman Düzenleme</span>
                )}
                {!editPermissions.canEditAll && !editPermissions.canEditChild && !editPermissions.canEditDepartment && (
                  <span className="px-2 py-1 bg-red-100 text-red-800 rounded">Düzenleme Yetkisi Yok</span>
                )}
              </div>
            )}
         </div>
      </div>

      {!userPermissions ? (
        <div className="bg-white border rounded-lg p-8 text-center">
          <div className="text-gray-500 mb-4">
            <div className="text-4xl mb-2">⏳</div>
            <div className="text-lg font-medium">Yetkiler yükleniyor...</div>
          </div>
        </div>
      ) : !userPermissions.canViewAll && !userPermissions.canViewChild && !userPermissions.canViewDepartment ? (
        <div className="bg-white border rounded-lg p-8 text-center">
          <div className="text-red-500 mb-4">
            <div className="text-4xl mb-2">🚫</div>
            <div className="text-lg font-medium">Görüntüleme Yetkiniz Yok</div>
            <div className="text-sm">Takım verilerini görüntülemek için gerekli yetkilere sahip değilsiniz.</div>
          </div>
        </div>
      ) : teamState.length === 0 ? (
        <div className="bg-white border rounded-lg p-8 text-center">
          <div className="text-gray-500 mb-4">
            <div className="text-4xl mb-2">🔍</div>
                         <div className="text-lg font-medium">
               {selectedDepartments.length > 0 || selectedRoles.length > 0 || searchTerm.trim() || selectedWorkStatus.length > 0 || startDate || endDate
                 ? 'Filtreleme kriterlerine uygun kişi bulunamadı' 
                 : 'Veri yüklenmedi'
               }
             </div>
             <div className="text-sm">
               {selectedDepartments.length > 0 || selectedRoles.length > 0 || searchTerm.trim() || selectedWorkStatus.length > 0 || startDate || endDate
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
                    const isHoliday = status === 5;
                    return (
                      <td key={dayIndex} className="p-3 border-r border-b text-center">
                        <div className="flex flex-col items-center gap-1">
                          <div 
                            className={`w-6 h-6 rounded-full border-2 ${style.bg} ${style.border} flex items-center justify-center ${isHoliday ? 'cursor-help' : ''}`}
                            title={isHoliday ? 'Resmi Tatil - Değiştirilemez' : ''}
                          >
                            <div className={`w-3 h-3 ${style.inner} rounded-full`}></div>
                          </div>
                          <div className={`text-xs ${isHoliday ? 'text-orange-600 font-medium' : 'text-gray-500'}`}>
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
                           disabled={isEditLoading || !canEditMember(member)}
                           className={`px-3 py-1 text-xs rounded transition-colors ${
                               isEditLoading || !canEditMember(member)
                                   ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                                   : 'bg-blue-500 text-white hover:bg-blue-600'
                           }`}
                           title={
                             !canEditMember(member)
                                 ? (isSelf(member)
                                     ? "Kendi attendance'ınızı takım görünümünden düzenleyemezsiniz"
                                     : "Bu kullanıcının attendance bilgisini düzenleme yetkiniz yok")
                                 : ''
                           }
                       >
                         {isEditLoading ? 'Yükleniyor...' : 'Düzenle'}
                       </button>
                       {!(member.approved || member.isApproved) && (
                           <button
                               onClick={() => handleApprove(member.id)}
                               disabled={!canEditMember(member)}
                               className={`px-3 py-1 text-xs rounded transition-colors ${
                                   !canEditMember(member)
                                       ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                                       : 'bg-green-500 text-white hover:bg-green-600'
                               }`}
                               title={
                                 !canEditMember(member)
                                     ? (isSelf(member)
                                         ? "Kendi attendance'ınızı takım görünümünden onaylayamazsınız"
                                         : "Bu kullanıcının attendance bilgisini onaylama yetkiniz yok")
                                     : ''
                               }
                           >
                             Onayla
                           </button>
                       )}
                       {(member.approved || member.isApproved) && (
                         <span className="px-3 py-1 text-xs bg-gray-200 text-gray-600 rounded">
                           Onaylandı
                         </span>
                       )}
                       {member.attendance.some(status => status === 5) && (
                         <span className="px-3 py-1 text-xs bg-orange-200 text-orange-600 rounded">
                           Tatil Günleri
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