import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import * as authService from '../services/authService';

const SesionContexto = createContext(null);
const EVENTO_SESION_EXPIRADA = 'praxis:sesion-expirada';

export function SesionProveedor({ children }) {
  const [sesion, setSesion] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let activo = true;
    authService.sesionActual()
      .then((datos) => {
        if (activo) setSesion(datos);
      })
      .catch(() => {
        if (activo) setSesion(null);
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, []);

  useEffect(() => {
    const expirada = () => setSesion(null);
    window.addEventListener(EVENTO_SESION_EXPIRADA, expirada);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, expirada);
  }, []);

  const iniciar = useCallback(async (correo, password) => {
    const datos = await authService.iniciarSesion(correo, password);
    setSesion(datos);
    return datos;
  }, []);

  const cerrar = useCallback(async () => {
    try {
      await authService.cerrarSesion();
    } finally {
      setSesion(null);
    }
  }, []);

  const tieneRol = useCallback((...roles) => roles.some((rol) => sesion?.roles?.includes(rol)), [sesion]);

  const valor = useMemo(() => ({ sesion, cargando, iniciar, cerrar, tieneRol }),
    [sesion, cargando, iniciar, cerrar, tieneRol]);

  return <SesionContexto.Provider value={valor}>{children}</SesionContexto.Provider>;
}

export function useSesion() {
  const contexto = useContext(SesionContexto);
  if (!contexto) {
    throw new Error('useSesion debe usarse dentro de SesionProveedor.');
  }
  return contexto;
}
