import React from 'react';

import { nombreRol } from '../../constantes';
import Boton from '../comunes/Boton';

export default function Encabezado({ usuario, onCerrarSesion }) {
  const roles = usuario?.roles || [];

  return (
    <header className="encabezado">
      <div className="encabezado__contenido">
        <div className="marca">
          <span className="marca__simbolo" aria-hidden="true">P</span>
          <div>
            <p className="marca__nombre">PRAXIS</p>
            <p className="marca__modulo">Gestión de prácticas preprofesionales</p>
          </div>
        </div>
        <div className="usuario">
          <div>
            <p className="usuario__nombre">{usuario?.nombres} {usuario?.apellidos}</p>
            <p className="usuario__rol">{roles.map(nombreRol).join(', ') || 'Sin roles asignados'}</p>
          </div>
          <Boton variante="secundario" pequeno onClick={onCerrarSesion}>
            Cerrar sesión
          </Boton>
        </div>
      </div>
    </header>
  );
}
