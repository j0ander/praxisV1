import React, { useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import { mensajeDeError } from '../../services/api';
import { actualizarEmpresa } from '../../services/empresaService';
import { validarDatosEmpresa } from './validacionesEmpresa';

function desdeEmpresa(empresa) {
  return {
    ruc: empresa.ruc || '',
    razonSocial: empresa.razonSocial || '',
    nombreComercial: empresa.nombreComercial || '',
    direccion: empresa.direccion || '',
    telefono: empresa.telefono || '',
    correo: empresa.correo || '',
    activa: Boolean(empresa.activa),
  };
}

export default function DatosEmpresa({ empresa, puedeEditar = true, onGuardada }) {
  const [formulario, setFormulario] = useState(() => desdeEmpresa(empresa));
  const [erroresCampos, setErroresCampos] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    setFormulario(desdeEmpresa(empresa));
    setErroresCampos({});
  }, [empresa]);

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  async function guardar(evento) {
    evento.preventDefault();
    const errores = validarDatosEmpresa(formulario);
    setErroresCampos(errores);
    if (Object.keys(errores).length > 0) return;

    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      await actualizarEmpresa({
        id: empresa.id,
        ruc: formulario.ruc.trim(),
        razonSocial: formulario.razonSocial.trim(),
        nombreComercial: formulario.nombreComercial.trim() || null,
        direccion: formulario.direccion.trim() || null,
        telefono: formulario.telefono.trim() || null,
        correo: formulario.correo.trim() || null,
        activa: Boolean(formulario.activa),
      });
      setResultado({ tipo: 'exito', mensaje: 'Datos de la empresa actualizados.' });
      if (onGuardada) onGuardada();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <>
      <h2>Datos de la empresa</h2>
      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} />
      <form onSubmit={guardar}>
        <div className="rejilla">
          <CampoFormulario
            id="datos-ruc"
            etiqueta="RUC"
            obligatorio
            value={formulario.ruc}
            onChange={(evento) => cambiarCampo('ruc', evento.target.value)}
            error={erroresCampos.ruc}
            disabled={!puedeEditar || guardando}
            maxLength={13}
          />
          <CampoFormulario
            id="datos-razon-social"
            etiqueta="Razón social"
            obligatorio
            value={formulario.razonSocial}
            onChange={(evento) => cambiarCampo('razonSocial', evento.target.value)}
            error={erroresCampos.razonSocial}
            disabled={!puedeEditar || guardando}
          />
          <CampoFormulario
            id="datos-nombre-comercial"
            etiqueta="Nombre comercial"
            value={formulario.nombreComercial}
            onChange={(evento) => cambiarCampo('nombreComercial', evento.target.value)}
            disabled={!puedeEditar || guardando}
          />
          <CampoFormulario
            id="datos-direccion"
            etiqueta="Dirección"
            value={formulario.direccion}
            onChange={(evento) => cambiarCampo('direccion', evento.target.value)}
            disabled={!puedeEditar || guardando}
          />
          <CampoFormulario
            id="datos-telefono"
            etiqueta="Teléfono"
            value={formulario.telefono}
            onChange={(evento) => cambiarCampo('telefono', evento.target.value)}
            disabled={!puedeEditar || guardando}
          />
          <CampoFormulario
            id="datos-correo"
            etiqueta="Correo"
            type="email"
            value={formulario.correo}
            onChange={(evento) => cambiarCampo('correo', evento.target.value)}
            disabled={!puedeEditar || guardando}
          />
        </div>
        <div className="opcion">
          <input
            id="datos-activa"
            type="checkbox"
            checked={formulario.activa}
            onChange={(evento) => cambiarCampo('activa', evento.target.checked)}
            disabled={!puedeEditar || guardando}
          />
          <label htmlFor="datos-activa">Empresa activa</label>
        </div>
        {puedeEditar ? (
          <div className="formulario__acciones">
            <Boton type="submit" cargando={guardando}>Guardar cambios</Boton>
          </div>
        ) : null}
      </form>
    </>
  );
}
