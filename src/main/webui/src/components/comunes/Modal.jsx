import React from 'react';

import Boton from './Boton';

export default function Modal({
  titulo,
  mensaje,
  confirmarTexto = 'Confirmar',
  onConfirmar,
  onCancelar,
  cargando = false,
}) {
  return (
    <div className="modal__fondo" role="presentation" onClick={onCancelar}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={titulo} onClick={(evento) => evento.stopPropagation()}>
        <h2>{titulo}</h2>
        <p>{mensaje}</p>
        <div className="modal__acciones">
          <Boton variante="secundario" onClick={onCancelar} disabled={cargando}>
            Cancelar
          </Boton>
          <Boton variante="peligro" onClick={onConfirmar} cargando={cargando}>
            {confirmarTexto}
          </Boton>
        </div>
      </div>
    </div>
  );
}
