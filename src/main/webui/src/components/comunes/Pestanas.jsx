import React from 'react';

export default function Pestanas({ items, activa, onChange }) {
  return (
    <div className="pestanas" role="tablist" aria-label="Secciones de la empresa">
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          role="tab"
          aria-selected={activa === item.id}
          className={`pestana${activa === item.id ? ' pestana--activa' : ''}`}
          onClick={() => onChange(item.id)}
        >
          {item.etiqueta}
        </button>
      ))}
    </div>
  );
}
