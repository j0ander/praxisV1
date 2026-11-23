import React, { useCallback, useEffect, useState } from 'react';

import MensajeEstado from '../../components/comunes/MensajeEstado';
import Pestanas from '../../components/comunes/Pestanas';
import RutaEnlace from '../../components/comunes/RutaEnlace';
import { useSesion } from '../../contextos/SesionContexto';
import { mensajeDeError } from '../../services/api';
import { buscarEmpresa } from '../../services/empresaService';
import ConveniosEmpresa from './ConveniosEmpresa';
import DatosEmpresa from './DatosEmpresa';
import MiembrosEmpresa from './MiembrosEmpresa';
import TutoresEmpresa from './TutoresEmpresa';

const PESTANAS = [
  { id: 'datos', etiqueta: 'Datos' },
  { id: 'convenios', etiqueta: 'Convenios' },
  { id: 'miembros', etiqueta: 'Miembros' },
  { id: 'tutores', etiqueta: 'Tutores empresariales' },
];

/**
 * UI-011. El contexto de navegación, no la mera presencia de roles, decide los
 * permisos de la pantalla: `/empresas/:id` es institucional y `/mi-empresa` es
 * el ámbito de la empresa validado por API-086.
 */
export default function DetalleEmpresa({ id, contexto }) {
  const { tieneRol } = useSesion();
  const rutaRetorno = contexto === 'miEmpresa' ? '/mi-empresa' : '/empresas';
  const esInstitucional = contexto === 'institucional'
    && tieneRol('ADMINISTRADOR', 'COORDINADOR');
  const esRepresentante = contexto === 'miEmpresa'
    && tieneRol('REPRESENTANTE_EMPRESA');
  const puedeEditarDatos = esInstitucional || esRepresentante;
  const puedeGestionarMiembros = esInstitucional || esRepresentante;
  const puedeGestionarConvenios = esInstitucional;
  const puedeGestionarTutores = esInstitucional;
  const [empresa, setEmpresa] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [pestana, setPestana] = useState('datos');

  // Al cambiar de empresa se vuelve siempre a la pestaña inicial.
  useEffect(() => {
    setPestana('datos');
  }, [id]);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    buscarEmpresa(id)
      .then((datos) => setEmpresa(datos))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, [id]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  if (!esInstitucional && !esRepresentante) {
    return (
      <>
        <RutaEnlace a={rutaRetorno} className="boton boton--secundario">Volver</RutaEnlace>
        <MensajeEstado tipo="error" mensaje="Contexto de acceso no válido para esta pantalla." />
      </>
    );
  }

  if (cargando && !empresa) {
    return <MensajeEstado mensaje="Cargando empresa…" />;
  }

  if (!empresa) {
    return (
      <>
        <RutaEnlace a={rutaRetorno} className="boton boton--secundario">
          {esInstitucional ? 'Volver al listado' : 'Volver a Mi empresa'}
        </RutaEnlace>
        <MensajeEstado tipo="error" mensaje={error} onReintentar={cargar} />
      </>
    );
  }

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>{empresa.razonSocial}</h1>
          <p>RUC {empresa.ruc}</p>
        </div>
        <RutaEnlace a={rutaRetorno} className="boton boton--secundario">
          {esInstitucional ? 'Volver al listado' : 'Volver a Mi empresa'}
        </RutaEnlace>
      </div>

      <MensajeEstado tipo="error" mensaje={error} onReintentar={cargar} />

      <Pestanas items={PESTANAS} activa={pestana} onChange={setPestana} />

      <div className="tarjeta" role="tabpanel">
        {pestana === 'datos' ? (
          <DatosEmpresa empresa={empresa} puedeEditar={puedeEditarDatos} onGuardada={cargar} />
        ) : null}
        {pestana === 'convenios' ? (
          <ConveniosEmpresa empresaId={empresa.id} puedeEditar={puedeGestionarConvenios} />
        ) : null}
        {pestana === 'miembros' ? (
          <MiembrosEmpresa empresaId={empresa.id} puedeGestionar={puedeGestionarMiembros} />
        ) : null}
        {pestana === 'tutores' ? (
          <TutoresEmpresa empresaId={empresa.id} puedeGestionar={puedeGestionarTutores} />
        ) : null}
      </div>
    </>
  );
}
