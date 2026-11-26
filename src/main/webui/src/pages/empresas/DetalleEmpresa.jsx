import React, { useCallback, useEffect, useState } from 'react';

import MensajeEstado from '../../components/comunes/MensajeEstado';
import Pestanas from '../../components/comunes/Pestanas';
import RutaEnlace from '../../components/comunes/RutaEnlace';
import { mensajeDeError } from '../../services/api';
import { buscarEmpresa, esAdministradorDeEmpresa, obtenerMembresia } from '../../services/empresaService';
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

export default function DetalleEmpresa({ id, usuarioId = null, soloMiEmpresa = false }) {
  const [empresa, setEmpresa] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [accesoDenegado, setAccesoDenegado] = useState(false);
  const [puedeGestionar, setPuedeGestionar] = useState(!soloMiEmpresa);
  const [pestana, setPestana] = useState('datos');

  useEffect(() => {
    setPestana('datos');
  }, [id]);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    setAccesoDenegado(false);
    setEmpresa(null);
    setPuedeGestionar(!soloMiEmpresa);
    try {
      if (soloMiEmpresa) {
        const membresia = await obtenerMembresia(usuarioId, id);
        if (!membresia) {
          setAccesoDenegado(true);
          return;
        }
        setPuedeGestionar(esAdministradorDeEmpresa(membresia));
      }
      const datos = await buscarEmpresa(id);
      setEmpresa(datos);
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
  }, [id, soloMiEmpresa, usuarioId]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  if (accesoDenegado) {
    return (
      <>
        <RutaEnlace a="/empresas" className="boton boton--secundario">Volver al listado</RutaEnlace>
        <MensajeEstado
          tipo="error"
          mensaje="Acceso no permitido: solo puede gestionar la empresa con la que está asociado."
        />
      </>
    );
  }

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
          <DatosEmpresa empresa={empresa} puedeEditar={puedeGestionar} onGuardada={cargar} />
        ) : null}
        {pestana === 'convenios' ? (
          <ConveniosEmpresa empresaId={empresa.id} puedeEditar={puedeGestionar} />
        ) : null}
        {pestana === 'miembros' ? (
          <MiembrosEmpresa empresaId={empresa.id} puedeGestionar={puedeGestionar} />
        ) : null}
        {pestana === 'tutores' ? (
          <TutoresEmpresa empresaId={empresa.id} puedeGestionar={puedeGestionar} />
        ) : null}
      </div>
    </>
  );
}
