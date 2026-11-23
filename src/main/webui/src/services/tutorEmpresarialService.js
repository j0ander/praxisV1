import { solicitar } from './api';

export function listarTutores(empresaId) {
  return solicitar(`/empresas/${empresaId}/tutores`);
}

export function crearTutor(empresaId, datos) {
  return solicitar(`/empresas/${empresaId}/tutores`, { metodo: 'POST', cuerpo: datos });
}

export function actualizarTutor(empresaId, datos) {
  return solicitar(`/empresas/${empresaId}/tutores`, { metodo: 'PUT', cuerpo: datos });
}
