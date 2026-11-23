import { solicitar } from './api';

export function listarMiembros(empresaId) {
  return solicitar(`/empresas/${empresaId}/miembros`);
}

export function vincularMiembro(empresaId, datos) {
  return solicitar(`/empresas/${empresaId}/miembros`, { metodo: 'POST', cuerpo: datos });
}

export function actualizarMiembro(empresaId, id, datos) {
  return solicitar(`/empresas/${empresaId}/miembros/${id}`, { metodo: 'PUT', cuerpo: datos });
}
