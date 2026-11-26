import React from 'react';

import Encabezado from './Encabezado';
import Navegacion from './Navegacion';
import PiePagina from './PiePagina';

export default function LayoutPrincipal({ children, modulos = [], usuario, onCerrarSesion }) {
  return (
    <div className="app">
      <Encabezado usuario={usuario} onCerrarSesion={onCerrarSesion} />
      <Navegacion modulos={modulos} />
      <main className="contenido">
        <div className="contenedor">{children}</div>
      </main>
      <PiePagina />
    </div>
  );
}
