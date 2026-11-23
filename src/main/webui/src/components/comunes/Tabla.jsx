import React from 'react';

import MensajeEstado from './MensajeEstado';

export default function Tabla({ columnas, filas, cargando = false, error = null, vacio = 'No hay registros.' }) {
  if (cargando) return <MensajeEstado mensaje="Cargando datos…" />;
  if (error) return <MensajeEstado tipo="error" mensaje={error} />;
  if (!filas || filas.length === 0) return <MensajeEstado mensaje={vacio} />;

  return (
    <div className="tabla-contenedor">
      <table className="tabla">
        <thead>
          <tr>
            {columnas.map((columna) => (
              <th key={columna.clave}>{columna.titulo}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, indice) => (
            <tr key={fila.id ?? indice}>
              {columnas.map((columna) => (
                <td key={columna.clave}>
                  {columna.render ? columna.render(fila) : fila[columna.clave]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
