import React, { useEffect, useState } from 'react';

import { mensajeDeError } from '../../services/api';
import { listarUsuarios } from '../../services/usuarioService';
import CampoFormulario from './CampoFormulario';
import MensajeEstado from './MensajeEstado';


export default function SelectorUsuarios({
  id,
  etiqueta = 'Usuario',
  valor,
  onChange,
  rol,
  excluirIds = [],
  obligatorio = false,
  error,
}) {
  const [usuarios, setUsuarios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState(null);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setErrorCarga(null);

    listarUsuarios({ rol, activo: true })
      .then((datos) => {
        if (activo) {
          setUsuarios(Array.isArray(datos) ? datos : []);
        }
      })
      .catch((fallo) => {
        if (activo) {
          setUsuarios([]);
          setErrorCarga(mensajeDeError(fallo));
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [rol]);

  const excluidos = new Set(excluirIds.map(String));
  const opciones = usuarios.filter((usuario) => !excluidos.has(String(usuario.id)));

  return (
    <CampoFormulario id={id} etiqueta={etiqueta} obligatorio={obligatorio} error={error}>
      <select
        id={id}
        value={valor}
        required={obligatorio}
        disabled={cargando || Boolean(errorCarga)}
        onChange={(evento) => onChange(evento.target.value)}
      >
        <option value="">{cargando ? 'Cargando usuarios…' : 'Seleccione un usuario'}</option>
        {opciones.map((usuario) => (
          <option key={usuario.id} value={usuario.id}>
            {usuario.nombres} {usuario.apellidos} · {usuario.correo}
          </option>
        ))}
      </select>
      {errorCarga ? <MensajeEstado tipo="error" mensaje={errorCarga} /> : null}
      {!cargando && !errorCarga && opciones.length === 0 ? (
        <small>No hay usuarios disponibles con el rol indicado.</small>
      ) : null}
    </CampoFormulario>
  );
}
