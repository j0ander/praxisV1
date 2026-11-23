import React from 'react';

import { SesionProveedor } from './contextos/SesionContexto';
import AppRoutes from './routes/AppRoutes';

export default function App() {
  return (
    <SesionProveedor>
      <AppRoutes />
    </SesionProveedor>
  );
}
