import React, { useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import { mensajeDeError } from '../../services/api';
import { iniciarSesion } from '../../services/authService';

export default function Login({ onIniciarSesion }) {
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function manejarEnvio(evento) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      const datos = await iniciarSesion(correo.trim(), password);
      onIniciarSesion(datos);
    } catch (fallo) {
      setError(fallo?.estado === 401
        ? 'Correo o contraseña incorrectos.'
        : mensajeDeError(fallo));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="pantalla-login">
      <div className="tarjeta-login">
        <div className="tarjeta-login__marca">
          <span className="tarjeta-login__simbolo" aria-hidden="true">P</span>
          <p className="tarjeta-login__nombre">PRAXIS</p>
        </div>
        <h1>Iniciar sesión</h1>
        <p>Ingrese con su correo y contraseña institucionales.</p>
        <MensajeEstado tipo="error" mensaje={error} />
        <form onSubmit={manejarEnvio}>
          <CampoFormulario
            id="correo"
            etiqueta="Correo electrónico"
            type="email"
            autoComplete="username"
            obligatorio
            value={correo}
            onChange={(evento) => setCorreo(evento.target.value)}
          />
          <CampoFormulario
            id="password"
            etiqueta="Contraseña"
            type="password"
            autoComplete="current-password"
            obligatorio
            value={password}
            onChange={(evento) => setPassword(evento.target.value)}
          />
          <Boton type="submit" cargando={enviando} disabled={!correo.trim() || !password}>
            Iniciar sesión
          </Boton>
        </form>
      </div>
    </div>
  );
}
