import React from 'react';

import { navegar } from '../../routes/navegacion';

export default function RutaEnlace({ a, children, className, onClick, ...propiedades }) {
  function manejarClic(evento) {
    if (evento.defaultPrevented || evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
      return;
    }
    evento.preventDefault();
    if (onClick) onClick();
    navegar(a);
  }

  return (
    <a href={a} className={className} onClick={manejarClic} {...propiedades}>
      {children}
    </a>
  );
}
