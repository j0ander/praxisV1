import React, { useState } from 'react';

import AppRoutes from './routes/AppRoutes';

export default function App() {
  const [usuario, setUsuario] = useState(null);

  return (
    <AppRoutes
      usuario={usuario}
      onIniciarSesion={setUsuario}
      onCerrarSesion={() => setUsuario(null)}
    />
  );
}
