import React from 'react';

export default function CampoFormulario({ etiqueta, id, obligatorio = false, error, children, ...propiedades }) {
  return (
    <div className="campo">
      <label htmlFor={id}>
        {etiqueta}
        {obligatorio ? ' *' : ''}
      </label>
      {children || <input id={id} {...propiedades} />}
      {error ? <small className="campo__error">{error}</small> : null}
    </div>
  );
}
