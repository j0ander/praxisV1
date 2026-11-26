import { solicitar } from './api';

export function listarEmpresas(texto = '') {
  return solicitar('/empresas/listarTodos', { parametros: { texto } });
}

export function buscarEmpresa(id) {
  return solicitar(`/empresas/buscar/${id}`);
}

export function crearEmpresa(datos) {
  return solicitar('/empresas', { metodo: 'POST', cuerpo: datos });
}

export function actualizarEmpresa(datos) {
  return solicitar('/empresas', { metodo: 'PUT', cuerpo: datos });
}
