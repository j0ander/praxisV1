import { solicitar } from './api';

export function listarUsuarios(filtros = {}) {
  return solicitar('/usuarios/listarTodos', { parametros: filtros });
}

export function buscarUsuario(id) {
  return solicitar(`/usuarios/buscar/${id}`);
}

export function crearUsuario(datos) {
  return solicitar('/usuarios', { metodo: 'POST', cuerpo: datos });
}

export function actualizarUsuario(datos) {
  return solicitar('/usuarios', { metodo: 'PUT', cuerpo: datos });
}

export function activarUsuario(id) {
  return solicitar(`/usuarios/${id}/activar`, { metodo: 'POST' });
}

export function desactivarUsuario(id) {
  return solicitar(`/usuarios/${id}/desactivar`, { metodo: 'POST' });
}

export function restablecerPassword(id, passwordTemporal) {
  return solicitar(`/usuarios/${id}/restablecerPassword`, {
    metodo: 'POST',
    cuerpo: { passwordTemporal },
  });
}

export function asignarRoles(id, roles) {
  return solicitar(`/usuarios/${id}/roles`, { metodo: 'PUT', cuerpo: { roles } });
}

export function asignarCarreras(id, carreraIds) {
  return solicitar(`/usuarios/${id}/carreras`, { metodo: 'PUT', cuerpo: { carreraIds } });
}

export function asignarCarreraCoordinador(id, carreraId) {
  return solicitar(`/usuarios/${id}/coordinador-carrera`, {
    metodo: 'PUT',
    cuerpo: { carreraId },
  });
}
