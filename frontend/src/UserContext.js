import React, { createContext, useContext, useState, useEffect } from "react";

const UserContext = createContext();

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);

  useEffect(() => {
    fetch('/api/me')
        .then(response => {
          if (!response.ok) throw new Error('Not authenticated');
          return response.json();
        })
        .then(data => setUser(data))
        .catch(error => {
          console.error('Kullanıcı bilgisi alınamadı:', error);
          setUser(null);
        });
  }, []);

  return (
      <UserContext.Provider value={{ user, setUser }}>
        {children}
      </UserContext.Provider>
  );
};
