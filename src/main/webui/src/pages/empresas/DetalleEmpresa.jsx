import React, { useCallback, useEffect, useState } from 'react';

import MensajeEstado from '../../components/comunes/MensajeEstado';
import Pestanas from '../../components/comunes/Pestanas';
import RutaEnlace from '../../components/comunes/RutaEnlace';
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

export default function DetalleEmpresa({ id }) {
  const [empresa, setEmpresa] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [pestana, setPestana] = useState('datos');

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

  if (cargando && !empresa) {
    return <MensajeEstado mensaje="Cargando empresa…" />;
  }

  if (!empresa) {
    return (
      <>
        <RutaEnlace a="/empresas" className="boton boton--secundario">Volver al listado</RutaEnlace>
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
        <RutaEnlace a="/empresas" className="boton boton--secundario">Volver al listado</RutaEnlace>
      </div>

      <MensajeEstado tipo="error" mensaje={error} onReintentar={cargar} />

      <Pestanas items={PESTANAS} activa={pestana} onChange={setPestana} />

      <div className="tarjeta" role="tabpanel">
        {pestana === 'datos' ? (
          <DatosEmpresa empresa={empresa} puedeEditar onGuardada={cargar} />
        ) : null}
        {pestana === 'convenios' ? (
          <ConveniosEmpresa empresaId={empresa.id} puedeEditar />
        ) : null}
        {pestana === 'miembros' ? (
          <MiembrosEmpresa empresaId={empresa.id} puedeGestionar />
        ) : null}
        {pestana === 'tutores' ? (
          <TutoresEmpresa empresaId={empresa.id} puedeGestionar />
        ) : null}
      </div>
    </>
  );
}
