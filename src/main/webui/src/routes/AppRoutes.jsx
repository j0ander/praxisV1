import React, { useEffect } from 'react';

import MensajeEstado from '../components/comunes/MensajeEstado';
import LayoutPrincipal from '../components/layout/LayoutPrincipal';
import Login from '../pages/auth/Login';
import Docentes from '../pages/docentes/Docentes';
import Facultades from '../pages/facultades/Facultades';
import Carreras from '../pages/carreras/Carreras';
import Periodos from '../pages/periodos/Periodos';
import TiposPractica from '../pages/tiposPractica/TiposPractica';
import DetalleEmpresa from '../pages/empresas/DetalleEmpresa';
import Empresas from '../pages/empresas/Empresas';
import Estudiantes from '../pages/estudiantes/Estudiantes';
import Inicio from '../pages/inicio/Inicio';
import FormularioUsuario from '../pages/usuarios/FormularioUsuario';
import Usuarios from '../pages/usuarios/Usuarios';
import { modulosPermitidos, primeraRutaPermitida, rutaPermitida } from '../permisos';
import { navegar, usarRuta } from './navegacion';

export default function AppRoutes({ usuario, onIniciarSesion, onCerrarSesion }) {
  const ruta = usarRuta();
  const roles = usuario?.roles ?? [];
  const modulos = modulosPermitidos(roles);
  const destino = primeraRutaPermitida(roles);
  const sinSesion = !usuario;
  const rutaInicial = ruta === '/' || ruta === '';
  const fueraDeAmbito = !rutaInicial && !rutaPermitida(ruta, roles);

  useEffect(() => {
    if (sinSesion) {
      if (ruta !== '/login') {
        navegar('/login', { reemplazar: true });
      }
      return;
    }
    if (rutaInicial || ruta === '/login') {
      navegar('/inicio', { reemplazar: true });
    } else if (fueraDeAmbito) {
      navegar(destino, { reemplazar: true });
    }
  }, [sinSesion, ruta, rutaInicial, fueraDeAmbito, destino]);

  if (sinSesion) {
    return ruta === '/login'
      ? <Login onIniciarSesion={onIniciarSesion} />
      : <MensajeEstado mensaje="Redirigiendo al inicio de sesión…" />;
  }

  if (rutaInicial || ruta === '/login' || fueraDeAmbito) {
    return <MensajeEstado mensaje="Redirigiendo…" />;
  }

  let pagina;

  if (ruta === '/inicio') {
    pagina = <Inicio modulos={modulos} />;
  } else if (ruta === '/admin/usuarios') {
    pagina = <Usuarios />;
  } else if (ruta === '/admin/usuarios/nuevo') {
    pagina = <FormularioUsuario />;
  } else if (/^\/admin\/usuarios\/\d+$/.test(ruta)) {
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
    pagina = <Estudiantes />;
  } else if (ruta === '/docentes') {
    pagina = <Docentes />;
  } else if (ruta === '/empresas') {
    pagina = <Empresas />;
  } else if (/^\/empresas\/\d+$/.test(ruta)) {
    pagina = <DetalleEmpresa id={ruta.split('/').pop()} />;
  } else {
    pagina = <MensajeEstado tipo="error" mensaje="Página no encontrada." />;
  }

  return (
    <LayoutPrincipal
      modulos={modulos}
      usuario={usuario}
      onCerrarSesion={onCerrarSesion}
    >
      {pagina}
    </LayoutPrincipal>
  );
}
