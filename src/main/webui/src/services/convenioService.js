import { solicitar } from './api';

export function listarConveniosPorEmpresa(empresaId) {
  return solicitar(`/convenios/listarPorEmpresa/${empresaId}`);
}

export function crearConvenio(datos) {
  return solicitar('/convenios', { metodo: 'POST', cuerpo: datos });
}

export function actualizarConvenio(datos) {
  return solicitar('/convenios', { metodo: 'PUT', cuerpo: datos });
}

export function suspenderConvenio(id) {
  return solicitar(`/convenios/${id}/suspender`, { metodo: 'POST' });
}

export function terminarConvenio(id) {
  return solicitar(`/convenios/${id}/terminar`, { metodo: 'POST' });
}
