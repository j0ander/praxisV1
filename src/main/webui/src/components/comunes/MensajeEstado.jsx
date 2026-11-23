import React from 'react';

import Boton from './Boton';

export default function MensajeEstado({ tipo = 'info', mensaje, onReintentar }) {
  if (!mensaje) return null;
  const clase = tipo === 'info' ? 'mensaje' : `mensaje mensaje--${tipo}`;

  return (
    <div className={clase} role={tipo === 'error' ? 'alert' : 'status'}>
      <span>{mensaje}</span>
      {onReintentar ? (
        <Boton variante="enlace" pequeno onClick={onReintentar}>
          Reintentar
        </Boton>
      ) : null}
    </div>
  );
}
