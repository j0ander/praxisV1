import { solicitar } from './api';

export function listarEstudiantes(carreraId) {
  return solicitar('/estudiantes/listarTodos', { parametros: { carreraId } });
}

export function buscarEstudiante(id) {
  return solicitar(`/estudiantes/buscar/${id}`);
}

export function crearEstudiante(datos) {
  return solicitar('/estudiantes', { metodo: 'POST', cuerpo: datos });
}

export function actualizarEstudiante(datos) {
  return solicitar('/estudiantes', { metodo: 'PUT', cuerpo: datos });
}

export function listarDocentes() {
  return solicitar('/docentes/listarTodos');
}

export function actualizarDocente(datos) {
  return solicitar('/docentes', { metodo: 'PUT', cuerpo: datos });
}
