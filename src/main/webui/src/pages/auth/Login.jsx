import React, { useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import { useSesion } from '../../contextos/SesionContexto';
import { mensajeDeError } from '../../services/api';
import { navegar } from '../../routes/navegacion';

export default function Login() {
  const { iniciar } = useSesion();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function manejarEnvio(evento) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await iniciar(correo, password);
      navegar('/inicio', { reemplazar: true });
    } catch (fallo) {
      setError(fallo?.estado === 401
        ? 'Credenciales incorrectas o cuenta no disponible.'
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
        <p>Ingrese sus credenciales institucionales.</p>
        <MensajeEstado tipo="error" mensaje={error} />
        <form onSubmit={manejarEnvio}>
          <CampoFormulario
            id="correo"
            etiqueta="Correo institucional"
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
          <Boton type="submit" cargando={enviando} disabled={!correo || !password}>
            Ingresar al sistema
          </Boton>
        </form>
      </div>
    </div>
  );
}
