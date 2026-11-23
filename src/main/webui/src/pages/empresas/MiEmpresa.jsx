import React from 'react';

import MensajeEstado from '../../components/comunes/MensajeEstado';
import DetalleEmpresa from './DetalleEmpresa';

/**
 * Contenedor de UI-011 para REP. El ámbito y el permiso ADMIN_EMPRESA ya fueron
 * validados por API-086 en AppRoutes; aquí solo se presenta el resultado.
 */
export default function MiEmpresa({ miEmpresa, onReintentar }) {
  if (miEmpresa.estado === 'verificando' || miEmpresa.estado === 'sin-verificar') {
    return <MensajeEstado mensaje="Verificando el acceso a Mi empresa…" />;
  }

  if (miEmpresa.estado !== 'autorizado' || !miEmpresa.empresaId) {
    return (
      <MensajeEstado
        tipo="error"
        mensaje={miEmpresa.error || 'No se pudo identificar la empresa del representante.'}
        onReintentar={onReintentar}
      />
    );
  }

  return <DetalleEmpresa id={miEmpresa.empresaId} contexto="miEmpresa" />;
}
