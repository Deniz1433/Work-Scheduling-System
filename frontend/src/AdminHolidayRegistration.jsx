import React, { useEffect, useState } from 'react';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';

const HolidayRegistration = () => {
    const [holidays, setHolidays] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [holidayName, setHolidayName] = useState('');
    const [holidayStart, setHolidayStart] = useState('');
    const [holidayEnd, setHolidayEnd] = useState('');
    const [success, setSuccess] = useState(null);
    const [fetchLoading, setFetchLoading] = useState(false);

    useEffect(() => {
        fetchHolidays();
    }, []);

    const fetchHolidays = () => {
        setLoading(true);
        fetch('/api/holidays')
            .then(res => {
                if (!res.ok) throw new Error('Tatil verisi alınamadı');
                return res.json();
            })
            .then(data => {
                setHolidays(data);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    };

    // Yıllık tatilleri çek
    const handleFetchYearlyHolidays = () => {
        setFetchLoading(true);
        setError(null);
        setSuccess(null);
        
        const currentYear = new Date().getFullYear();
        
        fetch(`/api/holidays/fetch?year=${currentYear}`, {
            method: 'POST'
        })
            .then(res => {
                if (!res.ok) throw new Error('Yıllık tatiller çekilemedi');
                return res.json();
            })
            .then(data => {
                setSuccess(`${data.length} adet tatil başarıyla çekildi!`);
                fetchHolidays(); // Liste yenileme
                setFetchLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setFetchLoading(false);
            });
    };

    // Önümüzdeki 5 senenin tatillerini çek
    const handleFetchNext5Years = () => {
        setFetchLoading(true);
        setError(null);
        setSuccess(null);
        
        const currentYear = new Date().getFullYear();
        const promises = [];
        
        for (let year = currentYear; year <= currentYear + 4; year++) {
            promises.push(
                fetch(`/api/holidays/fetch?year=${year}`, {
                    method: 'POST'
                }).then(res => {
                    if (!res.ok) throw new Error(`${year} yılı tatilleri çekilemedi`);
                    return res.json();
                })
            );
        }
        
        Promise.all(promises)
            .then(results => {
                const totalHolidays = results.reduce((sum, data) => sum + data.length, 0);
                setSuccess(`${totalHolidays} adet tatil başarıyla çekildi! (${currentYear}-${currentYear + 4})`);
                fetchHolidays(); // Liste yenileme
                setFetchLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setFetchLoading(false);
            });
    };

    // Sadece bu senenin tatillerini filtrele
    const currentYear = new Date().getFullYear();
    const currentYearHolidays = holidays.filter(holiday => {
        const holidayYear = new Date(holiday.date).getFullYear();
        return holidayYear === currentYear;
    });

    // Tüm tatil aralıklarını yyyy-mm-dd string olarak çıkar (UTC ile tam gün)
    const toUTCDate = (str) => {
        const [year, month, day] = str.split('-');
        return new Date(Date.UTC(year, month - 1, day));
    };
    
    // Tatil aralığını sadece yyyy-mm-dd string olarak karşılaştır
    const getDateStringsInRange = (start, end) => {
        const result = [];
        let current = start;
        while (current <= end) {
            result.push(current);
            // Bir sonraki günü bul
            const [y, m, d] = current.split('-').map(Number);
            const next = new Date(y, m - 1, d + 1);
            const yyyy = next.getFullYear();
            const mm = String(next.getMonth() + 1).padStart(2, '0');
            const dd = String(next.getDate()).padStart(2, '0');
            current = `${yyyy}-${mm}-${dd}`;
        }
        return result;
    };
    
    const holidayDateSet = new Set();
    holidays.forEach(h => {
        const start = h.date;
        const end = h.endDate || h.date;
        getDateStringsInRange(start, end).forEach(dateStr => holidayDateSet.add(dateStr));
    });

    // Takvimde tatil günlerini işaretle
    const tileClassName = ({ date, view }) => {
        if (view === 'month') {
            const yyyy = date.getFullYear();
            const mm = String(date.getMonth() + 1).padStart(2, '0');
            const dd = String(date.getDate()).padStart(2, '0');
            const dateStr = `${yyyy}-${mm}-${dd}`;
            return holidayDateSet.has(dateStr) ? 'holiday-tile' : null;
        }
    };

    // Tatil ekle
    const handleAddHoliday = () => {
        setError(null);
        setSuccess(null);
        if (!holidayName || !holidayStart) {
            setError('Tatil adı ve başlangıç tarihini doldurun.');
            return;
        }
        if (holidayEnd && holidayEnd < holidayStart) {
            setError('Bitiş tarihi, başlangıç tarihinden önce olamaz.');
            return;
        }
        fetch('/api/holidays', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: holidayName, date: holidayStart, endDate: holidayEnd || holidayStart, countryCode: 'TR' })
        })
            .then(res => {
                if (!res.ok) throw new Error('Tatil eklenemedi');
                return res.json();
            })
            .then(() => {
                setHolidayName('');
                setHolidayStart('');
                setHolidayEnd('');
                setSuccess('Tatil başarıyla eklendi!');
                fetchHolidays();
            })
            .catch(err => setError(err.message));
    };

    // Tatil sil
    const handleDeleteHoliday = (id) => {
        setError(null);
        setSuccess(null);
        fetch(`/api/holidays/${id}`, { method: 'DELETE' })
            .then(res => {
                if (!res.ok && res.status !== 204) throw new Error('Tatil silinemedi');
                setSuccess('Tatil silindi.');
                fetchHolidays();
            })
            .catch(err => setError(err.message));
    };

    return (
        <div className="flex flex-col items-center min-h-screen bg-gray-50 py-8">
            <h1 className="text-2xl font-bold mb-6 text-blue-800">Tatil Ekle / Düzenle</h1>
            <div className="flex flex-col md:flex-row gap-8 w-full max-w-5xl">
                {/* Sol: Tatil Ekleme Formu ve Liste */}
                <div className="flex-1 flex flex-col gap-6">

                    <div className="bg-white rounded-xl shadow-lg p-6 mb-2">
                        <h2 className="text-lg font-semibold mb-4 text-blue-700">Yeni Tatil Ekle</h2>
                        <div className="flex flex-col gap-4">
                            <input
                                type="text"
                                placeholder="Tatil Adı"
                                value={holidayName}
                                onChange={e => setHolidayName(e.target.value)}
                                className="border p-2 rounded focus:outline-blue-400"
                            />
                            <div className="flex gap-2">
                                <input
                                    type="date"
                                    value={holidayStart}
                                    onChange={e => setHolidayStart(e.target.value)}
                                    className="border p-2 rounded focus:outline-blue-400 flex-1"
                                    placeholder="Başlangıç Tarihi"
                                />
                                <input
                                    type="date"
                                    value={holidayEnd}
                                    onChange={e => setHolidayEnd(e.target.value)}
                                    className="border p-2 rounded focus:outline-blue-400 flex-1"
                                    placeholder="Bitiş Tarihi (opsiyonel)"
                                />
                            </div>
                            <button 
                                onClick={handleAddHoliday} 
                                disabled={fetchLoading}
                                className={`bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700 transition-all ${
                                fetchLoading 
                                    ? 'bg-gray-400 text-gray-200 cursor-not-allowed' 
                                    : 'bg-blue-600 text-white hover:bg-green-700'
                            }`}
                            >
                                
                                {fetchLoading ? (
                                <span className="flex items-center justify-center gap-2">
                                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Tatil Ekleniyor...
                                </span>
                            ) : (
                                `+ Tatil Ekle`
                            )}
                            </button>
                            <button 
                            onClick={handleFetchYearlyHolidays}
                            disabled={fetchLoading}
                            className={`w-full px-4 py-3 rounded-lg font-semibold transition-all ${
                                fetchLoading 
                                    ? 'bg-gray-400 text-gray-200 cursor-not-allowed' 
                                    : 'bg-green-600 text-white hover:bg-green-700'
                            }`}
                        >
                            {fetchLoading ? (
                                <span className="flex items-center justify-center gap-2">
                                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Tatiller Çekiliyor...
                                </span>
                            ) : (
                                `📅 ${currentYear} Yılı Tatillerini Çek`
                            )}
                        </button>
                        <button 
                            onClick={handleFetchNext5Years}
                            disabled={fetchLoading}
                            className={`w-full px-4 py-3 rounded-lg font-semibold transition-all ${
                                fetchLoading 
                                    ? 'bg-gray-400 text-gray-200 cursor-not-allowed' 
                                    : 'bg-purple-600 text-white hover:bg-purple-700'
                            }`}
                        >
                            {fetchLoading ? (
                                <span className="flex items-center justify-center gap-2">
                                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Tatiller Çekiliyor...
                                </span>
                            ) : (
                                `📅 Önümüzdeki 5 Yılın Tatillerini Çek`
                            )}
                        </button>
                        </div>
                        {error && <div className="mt-3 text-red-600 font-medium">{error}</div>}
                        {success && <div className="mt-3 text-green-600 font-medium">{success}</div>}
                    </div>
                    
                    <div className="bg-white rounded-xl shadow p-6">
                        <h2 className="text-lg font-semibold mb-4 text-blue-700">Kayıtlı Tatiller ({currentYear})</h2>
                        <ul className="divide-y divide-gray-200">
                            {currentYearHolidays.length === 0 && <li className="text-gray-500">Bu sene için tatil kaydı yok.</li>}
                            {currentYearHolidays.map((holiday) => (
                                <li key={holiday.id} className="flex items-center justify-between py-2 group">
                                    <span className="flex-1">{holiday.name} <span className="text-gray-500">- {holiday.date}{holiday.endDate && holiday.endDate !== holiday.date ? ` / ${holiday.endDate}` : ''}</span></span>
                                    <button
                                        onClick={() => handleDeleteHoliday(holiday.id)}
                                        className="ml-2 p-2 rounded-full bg-red-100 hover:bg-red-200 transition-colors"
                                        title="Sil"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-red-600 group-hover:text-red-800" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
                
                {/* Sağ: Takvim */}
                {/* Sağ: Takvim */}
                <div className="bg-white rounded-xl shadow-lg p-6 flex flex-col items-center min-w-[340px]">
                    <h2 className="text-lg font-semibold mb-4 text-blue-700">Tatil Takvimi</h2>
                    <Calendar
                        tileClassName={tileClassName}
                        calendarType="iso8601"
                        className="border-0 shadow-md rounded-xl"
                    />
                    <style>{`
                        .holiday-tile {
                            background: #ffbebe !important;
                            color: #b10000 !important;
                            border-radius: 50%;
                        }
                        .react-calendar {
                            width: 320px;
                            font-family: inherit;
                            border: none;
                        }
                        .react-calendar__tile--active {
                            background: #2563eb !important;
                            color: #fff !important;
                        }
                    `}</style>
                </div>
            </div>
        </div>
    );
};

export default HolidayRegistration;