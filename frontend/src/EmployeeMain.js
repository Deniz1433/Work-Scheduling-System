import React, { useState } from 'react';
import {
  User,
  Calendar1,
  UserCog,
  LogOut,
  Building
} from 'lucide-react';
import AttendanceRegistration from './EmployeeAttendanceRegistration';
import TeamAttendance from './EmployeeTeamAttendance';
import AdminManageUsers from './AdminManageUsers';
import AdminDepartmentManagement from './AdminDepartmentManagement';
import AdminRoleManagement from './AdminRoleManagement';
import AdminHolidayRegistration from './AdminHolidayRegistration';
import AdminDepartmentHierarchy from './AdminDepartmentHierarchy';
import logo from './assets/logo.png';
import { useUser } from './UserContext';

const EmployeeMain = () => {
  const { user } = useUser();
  const [activeView, setActiveView] = useState('registration');

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
      id: 'manageUsers',
      label: 'Kullanıcıları Yönet',
      icon: UserCog,
      component: AdminManageUsers
    },
    {
      id: 'departmentManagement',
      label: 'Departmanları Yönet',
      icon: Building,
      component: AdminDepartmentManagement
    },
    {
      id: 'adminRoleManagement',
      label: 'Rolleri Yönet',
      icon: UserCog,
      component: AdminRoleManagement
    },
    {
      id: 'adminHolidayRegistration',
      label: 'Tatilleri Yönet',
      icon: Calendar1,
      component: AdminHolidayRegistration
    },
    {
      id: 'departmentHierarchy',
      label: 'Departman Hiyerarşisi',
      icon: Building,
      component: AdminDepartmentHierarchy
    }
  ];

  const renderActiveComponent = () => {
    const activeItem = allNavigationItems.find(item => item.id === activeView);
    if (!activeItem) return null;
    const Component = activeItem.component;
    return (
        <div className="w-full">
          <Component user={user} />
        </div>
    );
  };

  const handleLogout = () => {
    window.location.href = '/logout';
  };

  return (
      <div className="flex h-screen bg-gray-100">
        {/* Sidebar */}
        <div className="w-64 bg-gray-800 text-white p-4 flex flex-col">
          <h1 className="text-xl font-bold mb-8">Ofis Günü Kayıt Sistemi</h1>
          <nav className="space-y-2 flex-1">
            {allNavigationItems.map(item => {
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

          <img src={logo} alt="Logo" className="w-max mt-auto mx-auto" />

          <button
              onClick={handleLogout}
              className="mt-6 w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all text-left bg-red-600 hover:bg-red-700 text-white font-medium"
          >
            <LogOut className="w-5 h-5" />
            Çıkış Yap
          </button>
        </div>

        {/* Main content */}
        <div className="flex-1 flex flex-col">
          <header className="bg-white shadow-sm border-b border-gray-200 px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-gray-800">
                  {allNavigationItems.find(item => item.id === activeView)?.label}
                </h2>
                <p className="text-sm text-gray-600">
                  {activeView === 'registration' && 'Ofiste çalışacağınız günleri belirleyin'}
                  {activeView === 'team' && 'Ekip üyelerinin ofis günlerini görüntüleyin'}
                  {/* add other descriptions as needed */}
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

          <main className="flex-1 overflow-auto px-6 py-4">
            {renderActiveComponent()}
          </main>
        </div>
      </div>
  );
};

export default EmployeeMain;
