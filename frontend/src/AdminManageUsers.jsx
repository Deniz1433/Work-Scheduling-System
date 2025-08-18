import React, { useEffect, useState } from 'react';
import DataGrid, {
    Column,
    Editing,
    Paging,
    FilterRow,
    HeaderFilter,
    Toolbar,
    Item as ToolbarItem,
} from 'devextreme-react/data-grid';
import SelectBox from 'devextreme-react/select-box';
import Button from 'devextreme-react/button';
import Popup from 'devextreme-react/popup';
import Form, { SimpleItem, GroupItem } from 'devextreme-react/form';
import Swal from 'sweetalert2';

const attendanceMap = {
    0: 'Yok',
    1: 'Ofiste',
    2: 'Uzaktan',
    3: 'İzinli',
    4: 'Mazeretli',
    5: 'Resmi Tatil'
};

const AdminManageUsers = () => {
    const [users, setUsers] = useState([]);
    const [roles, setRoles] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [popupVisible, setPopupVisible] = useState(false);
    const [selectedUser, setSelectedUser] = useState(null);
    const [attendanceRecords, setAttendanceRecords] = useState([]);
    const [editingAttendanceData, setEditingAttendanceData] = useState([]);
    const [newUser, setNewUser] = useState({
        firstName: '',
        lastName: '',
        email: '',
        username: '',
        password: '',
        roleId: null,
        departmentId: null
    });

    const fetchUsers = () => {
        fetch('/api/admin/users')
            .then(res => res.json())
            .then(setUsers);
    };

    const fetchRoles = () => {
        fetch('/api/roles')
            .then(res => res.json())
            .then(setRoles);
    };

    const fetchDepartments = () => {
        fetch('/api/departments')
            .then(res => res.json())
            .then(setDepartments);
    };

    useEffect(() => {
        fetchUsers();
        fetchRoles();
        fetchDepartments();
    }, []);

    const handleDeleteUser = (user) => {
        Swal.fire({
            title: 'Kullanıcıyı Sil',
            text: `${user.firstName} ${user.lastName} silinsin mi?`,
            icon: 'warning',
            showCancelButton: true,
        }).then(result => {
            if (result.isConfirmed) {
                fetch(`/api/admin/users/${user.keycloakId || user.id}`, { method: 'DELETE' })
                    .then(() => fetchUsers());
            }
        });
    };

    const handleAttendance = (user) => {
        fetch(`/api/attendance/user/${user.id}`)
            .then(res => res.json())
            .then(data => {
                const records = data.data?.attendanceRecords || [];
                setAttendanceRecords(records);
                setEditingAttendanceData(records);
                setSelectedUser(user);
                setPopupVisible(true);
            });
    };

    const handleUpdate = async (e) => {
        const updated = e.newData || {};
        const original = e.oldData || {};
        const updates = [];

        if (updated.roleId && updated.roleId !== original.roleId) {
            updates.push(
                fetch(`/api/admin/users/${e.key}/role`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ roleId: updated.roleId })
                })
            );
        }

        if (updated.departmentId && updated.departmentId !== original.departmentId) {
            updates.push(
                fetch(`/api/admin/users/${e.key}/department`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ departmentId: updated.departmentId })
                })
            );
        }

        try {
            await Promise.all(updates);
            await fetchUsers();
        } catch (error) {
            console.error("Update Error:", error);
            Swal.fire('Hata', 'Kullanıcı güncellenemedi', 'error');
            throw error;
        }
    };

    const handleAddUserWithAttendance = () => {
        fetch('/api/admin/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newUser)
        })
            .then(res => res.json())
            .then(async createdUser => {
                await fetch('/api/attendance', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId: createdUser.id })
                });
                fetchUsers();
                setNewUser({ firstName: '', lastName: '', email: '', username: '', password: '', roleId: null, departmentId: null });
            });
    };

    const handleAttendanceChange = (index, field, value) => {
        const updated = [...editingAttendanceData];
        updated[index][field] = parseInt(value);
        setEditingAttendanceData(updated);
    };

    const handleSaveAttendance = async () => {
        try {
            const response = await fetch(`/api/attendance/user/${selectedUser.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ attendanceRecords: editingAttendanceData })
            });

            if (response.ok) {
                Swal.fire('Başarılı', 'Attendance kayıtları güncellendi', 'success');
                setPopupVisible(false);
            } else {
                throw new Error('Güncelleme başarısız');
            }
        } catch (error) {
            console.error('Attendance güncelleme hatası:', error);
            Swal.fire('Hata', 'Attendance kayıtları güncellenirken hata oluştu', 'error');
        }
    };

    return (
        <div className="p-6">
            <h2 className="text-2xl font-semibold mb-4">Kullanıcıları Yönet</h2>

            <div className="bg-white shadow-md rounded p-6 mb-8 border border-gray-200">
                <h3 className="text-lg font-bold mb-2">Yeni Kullanıcı Ekle</h3>
                <Form
                    formData={newUser}
                    labelLocation="top"
                    onFieldDataChanged={e => setNewUser({ ...newUser, [e.dataField]: e.value })}
                    colCount={2}
                >
                    <SimpleItem dataField="firstName" editorType="dxTextBox" label={{ text: 'Ad' }} />
                    <SimpleItem dataField="lastName" editorType="dxTextBox" label={{ text: 'Soyad' }} />
                    <SimpleItem dataField="username" editorType="dxTextBox" label={{ text: 'Kullanıcı Adı' }} />
                    <SimpleItem dataField="email" editorType="dxTextBox" label={{ text: 'Email' }} />
                    <SimpleItem dataField="password" editorType="dxTextBox" editorOptions={{ mode: 'password' }} label={{ text: 'Şifre' }} />
                    <SimpleItem dataField="roleId" editorType="dxSelectBox" editorOptions={{
                        items: roles,
                        displayExpr: 'name',
                        valueExpr: 'id',
                        placeholder: 'Rol Seç'
                    }} label={{ text: 'Rol' }} />
                    <SimpleItem dataField="departmentId" editorType="dxSelectBox" editorOptions={{
                        items: departments,
                        displayExpr: 'name',
                        valueExpr: 'id',
                        placeholder: 'Departman Seç'
                    }} label={{ text: 'Departman' }} />
                </Form>
                <div className="mt-4">
                    <Button
                        text="Kullanıcı Ekle"
                        type="success"
                        stylingMode="contained"
                        onClick={handleAddUserWithAttendance}
                    />
                </div>
            </div>

            <DataGrid
                dataSource={users}
                keyExpr="id"
                showBorders={true}
                rowAlternationEnabled={true}
                hoverStateEnabled={true}
                columnAutoWidth={true}
                onRowUpdating={handleUpdate}
            >
                <FilterRow visible={true} />
                <HeaderFilter visible={true} />
                <Paging defaultPageSize={10} />
                <Editing
                    mode="row"
                    allowUpdating={true}
                    allowAdding={false}
                    allowDeleting={false}
                />
                <Column dataField="firstName" caption="Ad" />
                <Column dataField="lastName" caption="Soyad" />
                <Column dataField="email" caption="E-Posta" />
                <Column
                    dataField="roleId"
                    caption="Rol"
                    lookup={{ dataSource: roles, valueExpr: 'id', displayExpr: 'name' }}
                />
                <Column
                    dataField="departmentId"
                    caption="Departman"
                    lookup={{ dataSource: departments, valueExpr: 'id', displayExpr: 'name' }}
                />
                <Column
                    caption="İşlem"
                    cellRender={({ data }) => (
                        <div className="flex gap-2">
                            <Button icon="event" hint="Attendance" onClick={() => handleAttendance(data)} />
                            <Button icon="trash" type="danger" hint="Sil" onClick={() => handleDeleteUser(data)} />
                        </div>
                    )}
                />
            </DataGrid>

            <Popup
                visible={popupVisible}
                onHiding={() => setPopupVisible(false)}
                title={`${selectedUser?.firstName} ${selectedUser?.lastName} - Attendance Düzenle`}
                showCloseButton={true}
                width={800}
                height={500}
            >
                <div className="overflow-auto max-h-[400px]">
                    {editingAttendanceData.length === 0 ? (
                        <p className="text-center">Kayıt bulunamadı.</p>
                    ) : (
                        <table className="w-full text-sm border">
                            <thead>
                            <tr>
                                <th className="border p-1">Başlangıç</th>
                                <th className="border p-1">Pzt</th>
                                <th className="border p-1">Sal</th>
                                <th className="border p-1">Çrş</th>
                                <th className="border p-1">Prş</th>
                                <th className="border p-1">Cum</th>
                            </tr>
                            </thead>
                            <tbody>
                            {editingAttendanceData.map((r, i) => (
                                <tr key={i}>
                                    <td className="border p-1">{r.weekStart}</td>
                                    {['monday', 'tuesday', 'wednesday', 'thursday', 'friday'].map(day => (
                                        <td key={day} className="border p-1">
                                            <select
                                                value={r[day]}
                                                onChange={(e) => handleAttendanceChange(i, day, e.target.value)}
                                                className="border rounded px-1"
                                            >
                                                {Object.entries(attendanceMap).map(([val, label]) => (
                                                    <option key={val} value={val}>{label}</option>
                                                ))}
                                            </select>
                                        </td>
                                    ))}
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                    <div className="mt-4 text-right">
                        <Button text="Kaydet" type="default" stylingMode="contained" onClick={handleSaveAttendance} />
                    </div>
                </div>
            </Popup>
        </div>
    );
};

export default AdminManageUsers;
