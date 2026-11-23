import React from 'react';

import Encabezado from './Encabezado';
import Navegacion from './Navegacion';
import PiePagina from './PiePagina';

export default function LayoutPrincipal({ children, accesoMiEmpresa = false }) {
  return (
    <div className="app">
      <Encabezado />
      <Navegacion accesoMiEmpresa={accesoMiEmpresa} />
      <main className="contenido">
        <div className="contenedor">{children}</div>
      </main>
      <PiePagina />
    </div>
  );
}
