import React from 'react';

export default function Boton({
  children,
  variante = 'primario',
  pequeno = false,
  cargando = false,
  className = '',
  disabled,
  type = 'button',
  ...propiedades
}) {
  const clases = [
    'boton',
    variante === 'primario' ? '' : `boton--${variante}`,
    pequeno ? 'boton--pequeno' : '',
    className,
  ].filter(Boolean).join(' ');

  return (
    <button type={type} className={clases} disabled={disabled || cargando} {...propiedades}>
      {cargando ? 'Procesando…' : children}
    </button>
  );
}
