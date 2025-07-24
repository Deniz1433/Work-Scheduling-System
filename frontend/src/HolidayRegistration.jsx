import React, { useState } from 'react';

const HolidayRegistration = () => {
    const [holidays, setHolidays] = useState([]);
    const [holidayName, setHolidayName] = useState('');
    const [holidayDate, setHolidayDate] = useState('');

    const handleAddHoliday = (e) => {
        e.preventDefault();
        if (!holidayName || !holidayDate) return;

        const newHoliday = {
            id: holidays.length + 1,
            name: holidayName,
            date: holidayDate,
        };

        setHolidays([...holidays, newHoliday]);
        setHolidayName('');
        setHolidayDate('');
    };

    return (
        <div className="p-6 bg-white min-h-screen">
            <h1 className="text-xl font-semibold mb-4">Tatil Kaydı</h1>

            <form onSubmit={handleAddHoliday} className="flex flex-col gap-4 mb-6">
                <input
                    type="text"
                    placeholder="Tatil Adı"
                    value={holidayName}
                    onChange={(e) => setHolidayName(e.target.value)}
                    className="border p-2 rounded"
                />
                <input
                    type="date"
                    value={holidayDate}
                    onChange={(e) => setHolidayDate(e.target.value)}
                    className="border p-2 rounded"
                />
                <button type="submit" className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">
                    Tatili Ekle
                </button>
            </form>

            <h2 className="text-lg font-medium mb-2">Kayıtlı Tatiller</h2>
            <ul className="list-disc pl-5">
                {holidays.map((holiday) => (
                    <li key={holiday.id}>
                        {holiday.name} - {holiday.date}
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default HolidayRegistration;