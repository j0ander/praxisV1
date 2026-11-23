import { solicitar } from './api';

export function listarCarreras(parametros = {}) {
  return solicitar('/carreras/listarTodos', { parametros });
}

export function crearCarrera(datos) {
  return solicitar('/carreras', { metodo: 'POST', cuerpo: datos });
}

export function actualizarCarrera(datos) {
  return solicitar('/carreras', { metodo: 'PUT', cuerpo: datos });
}

export function listarPeriodos() {
  return solicitar('/periodos/listarTodos');
}

export function crearPeriodo(datos) {
  return solicitar('/periodos', { metodo: 'POST', cuerpo: datos });
}

export function actualizarPeriodo(datos) {
  return solicitar('/periodos', { metodo: 'PUT', cuerpo: datos });
}

export function activarPeriodo(id) {
  return solicitar(`/periodos/${id}/activar`, { metodo: 'POST' });
}

export function desactivarPeriodo(id) {
  return solicitar(`/periodos/${id}/desactivar`, { metodo: 'POST' });
}

export function listarTiposPractica() {
  return solicitar('/tiposPractica/listarTodos');
}

export function crearTipoPractica(datos) {
  return solicitar('/tiposPractica', { metodo: 'POST', cuerpo: datos });
}

export function actualizarTipoPractica(datos) {
  return solicitar('/tiposPractica', { metodo: 'PUT', cuerpo: datos });
}

export function listarFacultades() {
  return solicitar('/facultades/listarTodos');
}

export function crearFacultad(datos) {
  return solicitar('/facultades', { metodo: 'POST', cuerpo: datos });
}

export function actualizarFacultad(datos) {
  return solicitar('/facultades', { metodo: 'PUT', cuerpo: datos });
}
