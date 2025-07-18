import React from 'react';

const EmployeeTeamAttendance = () => {
  //Örnek data
  const teamData = [
    {
      id: 1,
      name: "AHMET EMIN",
      surname: "KAHRAMAN",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [true, true, true, true, true] // Pzt, Salı, Çrş, Perş, Cuma
    },
    {
      id: 2,
      name: "EMILETTIN YAVZ",
      surname: "ÜVE",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [true, true, false, false, true]
    },
    {
      id: 3,
      name: "CENK",
      surname: "KARAASLAN",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [false, true, true, true, false]
    },
    {
      id: 4,
      name: "ONUR",
      surname: "ÇİMEN",
      department: "JAVA YAZILIM GELİŞTİRME DEPARTMANI",
      attendance: [true, false, false, false, true]
    }
  ];
  const generateWeekDays = () => {
    const today = new Date();
    const currentWeekStart = new Date(today);
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Sunday
    currentWeekStart.setDate(diff);
    
    const weekDays = [];
    for (let i = 0; i < 5; i++) {
      const date = new Date(currentWeekStart);
      date.setDate(currentWeekStart.getDate() + i);
      weekDays.push(date);
    }
    return weekDays;
  };
  const weekDays = generateWeekDays()

  return (
    <div className="flex-1 p-6 bg-white">
      <div className="mb-6">
        <h2 className="text-xl  mb-2">Ekibinizin ofis günleri</h2>
        <div className="flex items-center gap-2 mb-4">
        </div>
      </div>

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
            </tr>
          </thead>
          <tbody>
            {teamData.map((member, memberIndex) => (
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
                </td>
                {member.attendance.map((isPresent, dayIndex) => (
                  <td key={dayIndex} className="p-3 border-r border-b text-center">
                    <div className="flex justify-center">
                      {isPresent ? (
                        <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                          <div className="w-3 h-3 bg-white rounded-full"></div>
                        </div>
                      ) : (
                        <div className="w-6 h-6 border-2 border-gray-400 rounded-full bg-white"></div>
                      )}
                    </div>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 text-sm text-gray-600">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 bg-green-500 rounded-full flex items-center justify-center">
              <div className="w-2 h-2 bg-white rounded-full"></div>
            </div>
            <span>Ofiste</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border-2 border-gray-400 rounded-full bg-white"></div>
            <span>Uzaktan</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmployeeTeamAttendance;