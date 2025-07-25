// src/EmployeeMain.jsx

import React, { useState, useEffect } from 'react';
import { User, Users, FileText, Calendar1, UserPlus, UserCog, LogOut } from 'lucide-react';
import AttendanceRegistration from './EmployeeAttendanceRegistration';
import TeamAttendance from './EmployeeTeamAttendance';
import ExcuseForm from './EmployeeExcuseForm';
import DepartmentInfo from './EmployeeDepartmentInfo';
import AdminAddUser from './AdminAddUser';
import AdminManageRoles from './AdminManageRoles';
import logo from './assets/logo.png';

const developmentMode = true; // ⬅️ Set to false for production

const EmployeeMain = () => {
  const [user, setUser] = useState(null);
  const [activeView, setActiveView] = useState(null);

  useEffect(() => {
    fetch('/api/me')
        .then(res => res.json())
        .then(data => {
          setUser(data);
          setActiveView('registration'); // default view after login
        })
        .catch(err => console.error('Kullanıcı bilgisi alınamadı:', err));
  }, []);

  const navigationConfig = [
    {
      id: 'registration',
      label: 'Ofis Günü Kayıt',
      icon: User,
      component: AttendanceRegistration,
      requiredAuthority: 'PERM_VIEW_ATTENDANCE'
    },
    {
      id: 'team',
      label: 'Ekip Takvimini Görüntüle',
      icon: Calendar1,
      component: TeamAttendance,
      requiredAuthority: 'PERM_VIEW_TEAM'
    },
    {
      id: 'excuse',
      label: 'Mazeret Belirt',
      icon: FileText,
      component: ExcuseForm,
      requiredAuthority: 'PERM_SUBMIT_EXCUSE'
    },
    {
      id: 'department',
      label: 'Departman Bilgisi',
      icon: Users,
      component: DepartmentInfo,
      requiredAuthority: 'PERM_VIEW_DEPARTMENT'
    },
    {
      id: 'adminAddUser',
      label: 'Kullanıcı Ekle - Çıkar',
      icon: UserPlus,
      component: AdminAddUser,
      requiredAuthority: 'PERM_ADMIN_MANAGE_USERS'
    },
    {
      id: 'manageRoles',
      label: 'Rolleri Yönet',
      icon: UserCog,
      component: AdminManageRoles,
      requiredAuthority: 'PERM_ADMIN_MANAGE_ROLES'
    }
  ];

  const filteredViews = navigationConfig.filter(view =>
      developmentMode || user?.authorities?.includes(view.requiredAuthority)
  );

  const renderActiveComponent = () => {
    const activeItem = filteredViews.find(item => item.id === activeView);
    if (activeItem) {
      const Component = activeItem.component;
      return <Component user={user} />;
    }
    return null;
  };

  const handleLogout = () => window.location.href = '/logout';

  return (
      <div className="flex h-screen bg-gray-100">
        {/* Sidebar */}
        <div className="w-64 bg-gray-800 text-white p-4 flex flex-col">
          <h1 className="text-xl font-bold mb-8">Ofis Günü Kayıt Sistemi</h1>

          <nav className="space-y-2">
            {filteredViews.map(item => {
              const Icon = item.icon;
              const isActive = activeView === item.id;
              return (
                  <button
                      key={item.id}
                      onClick={() => setActiveView(item.id)}
                      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all text-left ${
                          isActive
                              ? 'bg-blue-600 text-white shadow-lg'
                              : 'hover:bg-gray-700 text-gray-300 hover:text-white'
                      }`}
                  >
                    <Icon className="w-5 h-5" />
                    <span className="font-medium">{item.label}</span>
                  </button>
              );
            })}
          </nav>

          {/* User Info */}
          <div className="mt-8 pt-6 border-t border-gray-700">
            <div className="flex items-center gap-3 p-3 bg-gray-700 rounded-lg">
              <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center">
                <User className="w-5 h-5 text-white" />
              </div>
              <div className="mt-auto">
                <div className="font-medium text-sm">{user?.username || 'Kullanıcı'}</div>
                <div className="text-xs text-gray-400">{user?.email}</div>
              </div>
            </div>
          </div>

          <img src={logo} alt="Logo" className="w-max mt-auto mx-auto" />
          <button
              onClick={handleLogout}
              className="mt-6 w-full flex items-center gap-3 px-4 py-3 rounded-lg bg-red-600 hover:bg-red-700 text-white font-medium"
          >
            <LogOut className="w-5 h-5" />
            Çıkış Yap
          </button>
        </div>

        {/* Main Content */}
        <div className="flex-1 flex flex-col">
          <header className="bg-white shadow-sm border-b border-gray-200 px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-gray-800">
                  {filteredViews.find(item => item.id === activeView)?.label}
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
              <div className="text-sm text-gray-500">
                {new Date().toLocaleDateString('tr-TR', {
                  weekday: 'long',
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric'
                })}
              </div>
            </div>
          </header>

          <main className="flex-1 overflow-auto">{renderActiveComponent()}</main>
        </div>
      </div>
  );
};

export default EmployeeMain;
