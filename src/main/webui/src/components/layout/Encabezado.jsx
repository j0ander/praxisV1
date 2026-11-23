import React from 'react';

import { nombreRol } from '../../constantes';
import { useSesion } from '../../contextos/SesionContexto';
import Boton from '../comunes/Boton';

export default function Encabezado() {
  const { sesion, cerrar } = useSesion();
  const roles = (sesion?.roles || []).map(nombreRol).join(', ');

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
          <span className="usuario__nombre">{sesion?.nombres || sesion?.correo}</span>
          <span className="usuario__rol">{roles}</span>
          <Boton variante="secundario" pequeno onClick={cerrar}>
            Cerrar sesión
          </Boton>
        </div>
      </div>
    </header>
  );
}
