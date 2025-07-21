import React, { useState } from 'react';
import { Search } from 'lucide-react';

const DepartmentInfo = () => {
  const [nameSearch, setNameSearch] = useState('');
  const [departmentSearch, setDepartmentSearch] = useState('');

  // Sample data - replace with your actual data source
  const employeeData = [
    {
      id: 1,
      name: 'AHMET GÜREL',
      department: 'BİLİŞİM',
      position: 'JAVA YAZ/ÜST GELİŞTİRME UZMANI',
      '5/13/2025': true,
      '5/14/2025': false,
      '5/15/2025': true,
      '5/16/2025': true,
      '5/17/2025': false
    },
    {
      id: 2,
      name: 'AYŞEGÜL',
      department: 'KBRIMAN',
      position: 'JAVA YAZ/ÜST GELİŞTİRME UZMANI',
      '5/13/2025': true,
      '5/14/2025': true,
      '5/15/2025': false,
      '5/16/2025': true,
      '5/17/2025': false
    },
    {
      id: 3,
      name: 'EMRE ÇAKIR YANCI',
      department: 'BİLİŞİM',
      position: 'VERI YÖNETME SİSTEMLERİ UZMANI',
      '5/13/2025': true,
      '5/14/2025': true,
      '5/15/2025': true,
      '5/16/2025': false,
      '5/17/2025': true
    },
    {
      id: 4,
      name: 'CEYHUN',
      department: 'KBRIMAN/İÇT',
      position: 'JAVA YAZ/ÜST GELİŞTİRME UZMANI',
      '5/13/2025': true,
      '5/14/2025': false,
      '5/15/2025': true,
      '5/16/2025': true,
      '5/17/2025': false
    },
    {
      id: 5,
      name: 'TAL/EK',
      department: 'TAL/EK',
      position: 'JAVA YAZ/ÜST GELİŞTİRME UZMANI',
      '5/13/2025': true,
      '5/14/2025': true,
      '5/15/2025': false,
      '5/16/2025': true,
      '5/17/2025': true
    }
  ];

  const dateColumns = ['5/13/2025', '5/14/2025', '5/15/2025', '5/16/2025', '5/17/2025'];

  // Filter data based on search criteria
  const filteredData = employeeData.filter(employee => {
    const nameMatch = employee.name.toLowerCase().includes(nameSearch.toLowerCase());
    const departmentMatch = employee.department.toLowerCase().includes(departmentSearch.toLowerCase());
    return nameMatch && departmentMatch;
  });

  const renderAttendanceCell = (value, isHighlighted = false) => {
    const baseClasses = "w-6 h-6 rounded-full border-2 flex items-center justify-center";
    
    if (isHighlighted) {
      return (
        <div className="bg-red-500 p-1 rounded">
          <div className={`${baseClasses} ${value ? 'bg-black border-black' : 'bg-white border-gray-400'}`}>
            {value && <div className="w-3 h-3 bg-white rounded-full"></div>}
          </div>
        </div>
      );
    }
    
    return (
      <div className={`${baseClasses} ${value ? 'bg-black border-black' : 'bg-white border-gray-400'}`}>
        {value && <div className="w-3 h-3 bg-white rounded-full"></div>}
      </div>
    );
  };

  return (
    <div className="flex-1 p-6 bg-white">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-6">
        </div>

        {/* Search Section */}
        <div className="flex items-center gap-4 mb-6 bg-gray-50 p-4 rounded-lg">
          <div className="flex items-center gap-2">
            <Search className="w-4 h-4 text-gray-500" />
            <span className="text-sm font-medium">İsim sorgusu</span>
            <input
              type="text"
              value={nameSearch}
              onChange={(e) => setNameSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-32 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium">Departman Sorgusu</span>
            <select
              value={departmentSearch}
              onChange={(e) => setDepartmentSearch(e.target.value)}
              className="border border-gray-300 px-3 py-1 text-sm w-40 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">Tümü</option>
              <option value="BİLİŞİM">BİLİŞİM</option>
              <option value="KBRIMAN">KBRIMAN</option>
              <option value="KBRIMAN/İÇT">KBRIMAN/İÇT</option>
              <option value="TAL/EK">TAL/EK</option>
            </select>
          </div>
        </div>

        {/* Data Grid */}
        <div className="border border-gray-300 rounded-lg overflow-hidden">
          {/* Header */}
          <div className="bg-gray-100 border-b border-gray-300">
            <div className="grid grid-cols-8 gap-0">
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Ad</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Soyad</div>
              <div className="p-3 border-r border-gray-300 font-semibold text-sm bg-yellow-200">Departman</div>
              {dateColumns.map((date, index) => (
                <div key={date} className="p-3 border-r border-gray-300 font-semibold text-sm text-center bg-yellow-200 last:border-r-0">
                  {date}
                </div>
              ))}
            </div>
          </div>

          {/* Data Rows */}
          <div className="bg-white">
            {filteredData.map((employee, rowIndex) => (
              <div key={employee.id} className={`grid grid-cols-8 gap-0 border-b border-gray-200 hover:bg-gray-50 ${rowIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee.name.split(' ')[0]}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee.name.split(' ').slice(1).join(' ')}
                </div>
                <div className="p-3 border-r border-gray-200 text-sm">
                  {employee.department}
                </div>
                {dateColumns.map((date, dateIndex) => (
                  <div key={date} className="p-3 border-r border-gray-200 flex justify-center items-center last:border-r-0">
                    {renderAttendanceCell(
                      employee[date], 
                      employee.id === 4 && dateIndex === 3 // Highlight specific cell like in the image
                    )}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>

        {/* Legend or Info Section */}
        <div className="mt-4 flex items-center gap-6 text-sm text-gray-600">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-black rounded-full"></div>
            <span>Ofiste</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-white border-2 border-gray-400 rounded-full"></div>
            <span>Remote</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-red-500 rounded"></div>
            <span>Özel Durum</span>
          </div>
        </div>

        {/* Export Button */}
        <div className="mt-6 flex justify-end">
          <button className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors">
            Export
          </button>
        </div>
      </div>
    </div>
  );
};

export default DepartmentInfo;