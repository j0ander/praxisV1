import { solicitar } from './api';
import { listarMiembros } from './miembroService';

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

// Filtro de interfaz: el backend aún no recibe una identidad verificable
// (no fija praxis.usuario_id), por lo que no sustituye la autorización REST.
export async function obtenerMembresia(usuarioId, empresaId) {
  if (usuarioId === null || usuarioId === undefined || empresaId === null || empresaId === undefined) {
    return null;
  }
  const miembros = await listarMiembros(empresaId);
  if (!Array.isArray(miembros)) {
    return null;
  }
  return miembros.find(
    (miembro) => Number(miembro.usuarioId) === Number(usuarioId) && miembro.activo === true,
  ) || null;
}

export function esAdministradorDeEmpresa(miembro) {
  return Boolean(miembro) && miembro.rolEmpresa === 'ADMIN_EMPRESA';
}

export async function esEmpresaAsociada(usuarioId, empresaId) {
  return Boolean(await obtenerMembresia(usuarioId, empresaId));
}

export async function listarEmpresasAsociadas(usuarioId, texto = '') {
  if (usuarioId === null || usuarioId === undefined) {
    return [];
  }
  const empresas = await listarEmpresas(texto);
  if (!Array.isArray(empresas)) {
    return [];
  }
  const resultados = await Promise.all(empresas.map(async (empresa) => {
    const asociada = await esEmpresaAsociada(usuarioId, empresa.id);
    return asociada ? empresa : null;
  }));
  return resultados.filter((empresa) => empresa !== null);
}
