import React, { createContext, useContext, useState, useEffect } from "react";

const UserContext = createContext();

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  useEffect(() => {
    fetch('/api/userInfo/current')
      .then(response => response.json())
      .then(data => {
        console.log('User data received:', data);
        setUser(data);
      })
      .catch(error => console.error('Hata:', error));
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser }}>
      {children}
    </UserContext.Provider>
  );
};
