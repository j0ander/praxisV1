import { mensajeDeError, solicitar } from './api';

const MENSAJES_MI_EMPRESA = {
  403: 'No tiene permiso para acceder a «Mi empresa».',
  404: 'No se encontró una empresa asociada con permisos ADMIN_EMPRESA.',
  409: 'Existe más de una empresa asociada con permisos ADMIN_EMPRESA. No se seleccionó ninguna.',
};

/** Mensaje de API-086; el estado de acceso siempre lo decide el backend. */
export function mensajeErrorMiEmpresa(fallo) {
  return MENSAJES_MI_EMPRESA[fallo?.estado] || mensajeDeError(fallo);
}

export function listarEmpresas(texto = '') {
  return solicitar('/empresas/listarTodos', { parametros: { texto } });
}

export function buscarEmpresa(id) {
  return solicitar(`/empresas/buscar/${id}`);
}

export function obtenerMiEmpresa() {
  return solicitar('/empresas/miEmpresa');
}

export function crearEmpresa(datos) {
  return solicitar('/empresas', { metodo: 'POST', cuerpo: datos });
}

export function actualizarEmpresa(datos) {
  return solicitar('/empresas', { metodo: 'PUT', cuerpo: datos });
}
