import React from 'react';

import MensajeEstado from '../../components/comunes/MensajeEstado';
import RutaEnlace from '../../components/comunes/RutaEnlace';

export default function Inicio({ modulos = [] }) {
  return (
    <>
      <h1>Bienvenido</h1>

      <section className="tarjeta">
        <h2>Módulo de usuarios y perfiles académicos</h2>
        <p>
          Este módulo permite consultar y gestionar cuentas, roles, carreras y perfiles académicos.
        </p>
      </section>

      <section className="tarjeta">
        <h2>Accesos disponibles</h2>
        {modulos.length === 0 ? (
          <MensajeEstado mensaje="No hay módulos disponibles para este perfil." />
        ) : (
          <div className="accesos">
            {modulos.map((acceso) => (
              <RutaEnlace key={acceso.ruta} a={acceso.ruta} className="acceso">
                <strong>{acceso.etiqueta}</strong>
                <span>{acceso.detalle}</span>
              </RutaEnlace>
            ))}
          </div>
        )}
      </section>
    </>
  );
}
