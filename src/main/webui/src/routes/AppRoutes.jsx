import React, { useEffect, useState } from 'react';

import MensajeEstado from '../components/comunes/MensajeEstado';
import LayoutPrincipal from '../components/layout/LayoutPrincipal';
import { useSesion } from '../contextos/SesionContexto';
import { mensajeErrorMiEmpresa, obtenerMiEmpresa } from '../services/empresaService';
import Docentes from '../pages/docentes/Docentes';
import Facultades from '../pages/facultades/Facultades';
import Carreras from '../pages/carreras/Carreras';
import Periodos from '../pages/periodos/Periodos';
import TiposPractica from '../pages/tiposPractica/TiposPractica';
import DetalleEmpresa from '../pages/empresas/DetalleEmpresa';
import Empresas from '../pages/empresas/Empresas';
import MiEmpresa from '../pages/empresas/MiEmpresa';
import Estudiantes from '../pages/estudiantes/Estudiantes';
import Inicio from '../pages/inicio/Inicio';
import Login from '../pages/auth/Login';
import FormularioUsuario from '../pages/usuarios/FormularioUsuario';
import Usuarios from '../pages/usuarios/Usuarios';
import { navegar, usarRuta } from './navegacion';

function RutaPrivada({ roles, children }) {
  const { tieneRol } = useSesion();
  if (roles && !tieneRol(...roles)) {
    return <MensajeEstado tipo="error" mensaje="No tiene permiso para esta operación." />;
  }
  return children;
}

const MI_EMPRESA_SIN_VERIFICAR = { estado: 'sin-verificar', empresaId: null, error: null };

export default function AppRoutes() {
  const { sesion, cargando, tieneRol } = useSesion();
  const ruta = usarRuta();
  const esRepresentante = tieneRol('REPRESENTANTE_EMPRESA');
  const [miEmpresa, setMiEmpresa] = useState(MI_EMPRESA_SIN_VERIFICAR);
  const [intentoMiEmpresa, setIntentoMiEmpresa] = useState(0);

  // Una sola comprobación de API-086 alimenta el menú y el contenedor de UI-011.
  useEffect(() => {
    if (cargando || !esRepresentante) {
      setMiEmpresa(MI_EMPRESA_SIN_VERIFICAR);
      return undefined;
    }
    let activo = true;
    setMiEmpresa({ estado: 'verificando', empresaId: null, error: null });
    obtenerMiEmpresa()
      .then((datos) => {
        if (!activo) return;
        setMiEmpresa(datos?.empresaId
          ? { estado: 'autorizado', empresaId: datos.empresaId, error: null }
          : { estado: 'denegado', empresaId: null,
              error: 'No se pudo identificar la empresa del representante.' });
      })
      .catch((fallo) => {
        if (activo) {
          setMiEmpresa({ estado: 'denegado', empresaId: null, error: mensajeErrorMiEmpresa(fallo) });
        }
      });
    return () => { activo = false; };
  }, [cargando, esRepresentante, intentoMiEmpresa]);

  const reintentarMiEmpresa = () => setIntentoMiEmpresa((numero) => numero + 1);

  useEffect(() => {
    if (cargando) return;
    if (ruta === '/' || ruta === '') {
      navegar(sesion ? '/inicio' : '/login', { reemplazar: true });
    } else if (sesion && ruta === '/login') {
      navegar('/inicio', { reemplazar: true });
    } else if (!sesion && ruta !== '/login') {
      navegar('/login', { reemplazar: true });
    }
  }, [cargando, sesion, ruta]);

  if (cargando || ruta === '/' || ruta === '') {
    return <MensajeEstado mensaje="Cargando sesión…" />;
  }

  if (ruta === '/login') {
    return <Login />;
  }

  let pagina;
  let roles;

  if (ruta === '/inicio') {
    pagina = <Inicio />;
  } else if (ruta === '/admin/usuarios') {
    roles = ['ADMINISTRADOR'];
    pagina = <Usuarios />;
  } else if (ruta === '/admin/usuarios/nuevo') {
    roles = ['ADMINISTRADOR'];
    pagina = <FormularioUsuario />;
  } else if (/^\/admin\/usuarios\/\d+$/.test(ruta)) {
    roles = ['ADMINISTRADOR'];
    pagina = <FormularioUsuario id={ruta.split('/').pop()} />;
  } else if (ruta === '/admin/facultades') {
    pagina = <Facultades />;
  } else if (ruta === '/admin/carreras') {
    pagina = <Carreras />;
  } else if (ruta === '/admin/periodos') {
    pagina = <Periodos />;
  } else if (ruta === '/admin/tipos-practica') {
    pagina = <TiposPractica />;
  } else if (ruta === '/estudiantes') {
    roles = ['ADMINISTRADOR', 'COORDINADOR'];
    pagina = <Estudiantes />;
  } else if (ruta === '/docentes') {
    roles = ['ADMINISTRADOR', 'COORDINADOR'];
    pagina = <Docentes />;
  } else if (ruta === '/empresas') {
    roles = ['ADMINISTRADOR', 'COORDINADOR'];
    pagina = <Empresas />;
  } else if (ruta === '/mi-empresa') {
    roles = ['REPRESENTANTE_EMPRESA'];
    pagina = <MiEmpresa miEmpresa={miEmpresa} onReintentar={reintentarMiEmpresa} />;
  } else if (/^\/empresas\/\d+$/.test(ruta)) {
    roles = ['ADMINISTRADOR', 'COORDINADOR'];
    pagina = <DetalleEmpresa id={ruta.split('/').pop()} contexto="institucional" />;
  } else {
    pagina = <MensajeEstado tipo="error" mensaje="Página no encontrada." />;
  }

  return (
    <LayoutPrincipal accesoMiEmpresa={miEmpresa.estado === 'autorizado'}>
      <RutaPrivada roles={roles}>{pagina}</RutaPrivada>
    </LayoutPrincipal>
  );
}
