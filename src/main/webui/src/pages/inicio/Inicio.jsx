import React from 'react';

import RutaEnlace from '../../components/comunes/RutaEnlace';
import { nombreRol } from '../../constantes';
import { useSesion } from '../../contextos/SesionContexto';

export default function Inicio() {
  const { sesion, tieneRol } = useSesion();
  const accesos = [];

  if (tieneRol('ADMINISTRADOR')) {
    accesos.push({ ruta: '/admin/usuarios', titulo: 'Usuarios', detalle: 'Cuentas, roles y carreras.' });
  }
  if (tieneRol('ADMINISTRADOR', 'COORDINADOR')) {
    accesos.push({ ruta: '/estudiantes', titulo: 'Estudiantes', detalle: 'Perfiles académicos de estudiantes.' });
    accesos.push({ ruta: '/docentes', titulo: 'Docentes', detalle: 'Perfiles académicos de docentes.' });
  }

  const roles = (sesion?.roles || []).map(nombreRol).join(', ');

  return (
    <>
      <h1>Bienvenido, {sesion?.nombres || sesion?.correo}</h1>
      <p>Correo: {sesion?.correo}</p>
      <p>Rol: {roles}</p>

      <section className="tarjeta">
        <h2>Módulo de usuarios y perfiles académicos</h2>
        <p>
          Este módulo permite consultar y gestionar cuentas, roles, carreras y perfiles académicos
          según los permisos de su rol.
        </p>
      </section>

      <section className="tarjeta">
        <h2>Accesos disponibles</h2>
        {accesos.length === 0 ? (
          <p>No hay módulos disponibles para su rol en esta entrega.</p>
        ) : (
          <div className="accesos">
            {accesos.map((acceso) => (
              <RutaEnlace key={acceso.ruta} a={acceso.ruta} className="acceso">
                <strong>{acceso.titulo}</strong>
                <span>{acceso.detalle}</span>
              </RutaEnlace>
            ))}
          </div>
        )}
      </section>
    </>
  );
}
