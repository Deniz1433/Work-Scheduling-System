import React, { useState } from 'react';
import { User, Users, FileText } from 'lucide-react';
import AttendanceRegistration from './EmployeeAttendanceRegistration';
import TeamAttendance from './EmployeeTeamAttendance';
import ExcuseForm from './EmployeeExcuseForm';
import logo from './assets/logo.png'

const EmployeeMain = () => {
  const [activeView, setActiveView] = useState('registration');

  const navigationItems = [
    {
      id: 'registration',
      label: 'Ofis Günü Kayıt',
      icon: User,
      component: AttendanceRegistration
    },
    {
      id: 'team',
      label: 'Ekip Takvimini Görüntüle',
      icon: Users,
      component: TeamAttendance
    },
    {
      id: 'excuse',
      label: 'Mazeret Belirt',
      icon: FileText,
      component: ExcuseForm
    }
  ];

  const renderActiveComponent = () => {
    const activeItem = navigationItems.find(item => item.id === activeView);
    if (activeItem) {
      const Component = activeItem.component;
      return <Component />;
    }
    return null;
  };

  return (
    <div className="flex h-screen bg-gray-100">
      {/*Navigasyon Çubuğu*/}
      <div className="w-64 bg-gray-800 text-white p-4 flex flex-col">
        <h1 className="text-xl font-bold mb-8">Ofis Günü Kayıt Sistemi</h1>
        
        <nav className="space-y-2">
          {navigationItems.map((item) => {
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

        {/*Kullanıcı bilgisi*/}
        <div className="mt-8 pt-6 border-t border-gray-700">
          <div className="flex items-center gap-3 p-3 bg-gray-700 rounded-lg">
            <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center">
              <User className="w-5 h-5 text-white" />
            </div>
            <div className="mt-auto">
              <div className="font-medium text-sm">Kullanıcı</div>
              <div className="text-xs text-gray-400">user@company.com</div>
            </div>
          </div>
        </div>

        {/*Yaşar Bilgi Logosu*/}
        <img src={logo} alt="Logo" className="w-max mt-auto mx-auto" />
      </div>

      {/*Ana ekran*/}
      <div className="flex-1 flex flex-col">
        {/*Başlık*/}
        <header className="bg-white shadow-sm border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-800">
                {navigationItems.find(item => item.id === activeView)?.label}
              </h2>
              <p className="text-sm text-gray-600">
                {activeView === 'registration' && 'Ofiste çalışacağınız günleri belirleyin'}
                {activeView === 'team' && 'Ekip üyelerinin ofis günlerini görüntüleyin'}
                {activeView === 'excuse' && 'Devamsızlık için mazeret başvurusu oluşturun'}
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