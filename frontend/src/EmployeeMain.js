import React, { useState, useEffect } from 'react';
import { User, Users, FileText, Calendar1, UserPlus, UserCog, LogOut } from 'lucide-react';
import AttendanceRegistration from './EmployeeAttendanceRegistration';
import TeamAttendance from './EmployeeTeamAttendance';
import ExcuseForm from './EmployeeExcuseForm';
import DepartmentInfo from './EmployeeDepartmentInfo';
import logo from './assets/logo.png';
import { useUser } from "./UserContext";
import AdminManageUsers from './AdminManageUsers';

const EmployeeMain = () => {
  const {user} = useUser();
  const [activeView, setActiveView] = useState('registration');

  // Tüm menü elemanları
  const allNavigationItems = [
    {
      id: 'registration',
      label: 'Ofis Günü Kayıt',
      icon: User,
      component: AttendanceRegistration
    },
    {
      id: 'team',
      label: 'Ekip Takvimini Görüntüle',
      icon: Calendar1,
      component: TeamAttendance
    },
    {
      id: 'excuse',
      label: 'Mazeret Belirt',
      icon: FileText,
      component: ExcuseForm
    },
    {
      id: 'department',
      label: 'Departman Bilgileri',
      icon: Users,
      component: DepartmentInfo
    },
    {
      id: 'manageUsers',
      label: 'Kullanıcıları Yönet',
      icon: UserCog,
      component: AdminManageUsers
    }
  ];

  // Kullanıcı rolüne göre buton filtreleme
  const navigationItems = allNavigationItems.filter(item => {
    if (item.id === 'department') {
      return user?.authorities?.includes('ROLE_attendance_client_manager');
    }
    if (item.id === 'team') {
      return (
          user?.authorities?.includes('ROLE_attendance_client_staff') ||
          user?.authorities?.includes('ROLE_attendance_client_team_leader')
      );
    }
    if (item.id === 'adminAddUser' || item.id === 'manageRoles') {
      return (
          user?.authorities?.includes('ROLE_attendance_client_admin') ||
          user?.authorities?.includes('ROLE_attendance_client_superadmin')
      );
    }
    return true;
  });

  const renderActiveComponent = () => {
    const activeItem = navigationItems.find(item => item.id === activeView);
    if (activeItem) {
      const Component = activeItem.component;
      return <Component user={user} />;
    }
    return null;
  };

  const handleLogout = () => {
    window.location.href = '/logout';
  };

  return (
      <div className="flex h-screen bg-gray-100">
        {/* Navigasyon Çubuğu */}
        <div className="w-64 bg-gray-800 text-white p-4 flex flex-col">
          <h1 className="text-xl font-bold mb-8">Ofis Günü Kayıt Sistemi</h1>

          <nav className="space-y-2">
            {navigationItems.map(item => {
              const Icon = item.icon;
              return (
                  <button
                      key={item.id}
                      onClick={() => setActiveView(item.id)}
                      className={`
                  w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all text-left
                  ${activeView === item.id
                          ? 'bg-blue-600 text-white shadow-lg'
                          : 'hover:bg-gray-700 text-gray-300 hover:text-white'
                      }
                `}
                  >
                    <Icon className="w-5 h-5" />
                    <span className="font-medium">{item.label}</span>
                  </button>
              );
            })}
          </nav>

          {/* Kullanıcı bilgisi */}
          <div className="mt-8 pt-6 border-t border-gray-700">
            <div className="flex items-center gap-3 p-3 bg-gray-700 rounded-lg">
              <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center">
                <User className="w-5 h-5 text-white" />
              </div>
              <div className="mt-auto">
                <div className="font-medium text-sm">
                  {user?.preferredUsername || user?.name || 'Kullanıcı'}
                </div>
                <div className="text-xs text-gray-400">
                  {user?.email || 'example@example.com'}
                </div>
              </div>
            </div>
          </div>

          {/* Yaşar Bilgi Logosu */}
          <img src={logo} alt="Logo" className="w-max mt-auto mx-auto" />

          {/* Çıkış Yap Butonu */}
          <button
              onClick={handleLogout}
              className="mt-6 w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all text-left bg-red-600 hover:bg-red-700 text-white font-medium"
          >
            <LogOut className="w-5 h-5" />
            Çıkış Yap
          </button>
        </div>

        {/* Ana ekran */}
        <div className="flex-1 flex flex-col">
          {/* Başlık */}
          <header className="bg-white shadow-sm border-b border-gray-200 px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-gray-800">
                  {navigationItems.find(item => item.id === activeView)?.label}
                </h2>
                <p className="text-sm text-gray-600">
                  {activeView === 'registration' && 'Ofiste çalışacağınız günleri belirleyin'}
                  {activeView === 'excuse' && 'Devamsızlık için mazeret başvurusu oluşturun'}
                  {activeView === 'team' && 'Ekip üyelerinin ofis günlerini görüntüleyin'}
                  {activeView === 'department' && 'Departman bilgilerini görüntüleyin'}
                  {activeView === 'adminAddUser' && 'Kullanıcı ekleyin - çıkarın'}
                  {activeView === 'manageRoles' && 'Kullanıcı rollerini yönetin'}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <div className="text-sm text-gray-500">
                  {new Date().toLocaleDateString('tr-TR', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                  })}
                </div>
              </div>
            </div>
          </header>

          {/* Dynamic Content */}
          <main className="flex-1 overflow-auto">
            {renderActiveComponent()}
          </main>
        </div>
      </div>
  );
};

export default EmployeeMain;
