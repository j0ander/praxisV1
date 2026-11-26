import React from 'react';

import { usarRuta } from '../../routes/navegacion';
import RutaEnlace from '../comunes/RutaEnlace';

const OPCION_INICIO = { ruta: '/inicio', etiqueta: 'Inicio' };

export default function Navegacion({ modulos = [] }) {
  const ruta = usarRuta();
  const opciones = [OPCION_INICIO, ...modulos];

  return (
    <nav className="navegacion" aria-label="Navegación principal">
      <ul className="navegacion__lista">
        {opciones.map((opcion) => {
          const activa = ruta === opcion.ruta
            || (opcion.ruta !== '/inicio' && ruta.startsWith(`${opcion.ruta}/`));
          return (
            <li key={opcion.ruta}>
              <RutaEnlace
                a={opcion.ruta}
                className={`navegacion__enlace${activa ? ' navegacion__enlace--activo' : ''}`}
                aria-current={activa ? 'page' : undefined}
              >
                {opcion.etiqueta}
              </RutaEnlace>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
