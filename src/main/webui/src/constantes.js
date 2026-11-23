export const ROLES = [
  'ADMINISTRADOR',
  'COORDINADOR',
  'ESTUDIANTE',
  'DOCENTE',
  'TUTOR_EMPRESARIAL',
  'REPRESENTANTE_EMPRESA',
  'AUTORIDAD',
];

export const NOMBRES_ROL = {
  ADMINISTRADOR: 'Administrador',
  COORDINADOR: 'Coordinador',
  ESTUDIANTE: 'Estudiante',
  DOCENTE: 'Docente tutor',
  TUTOR_EMPRESARIAL: 'Tutor empresarial',
  REPRESENTANTE_EMPRESA: 'Representante empresarial',
  AUTORIDAD: 'Autoridad',
};

export const TIPOS_DOCUMENTO = ['CEDULA', 'PASAPORTE'];

export const ROLES_EMPRESA = ['ADMIN_EMPRESA', 'PUBLICADOR'];

export const NOMBRES_ROL_EMPRESA = {
  ADMIN_EMPRESA: 'Administrador de empresa',
  PUBLICADOR: 'Publicador',
};

export function nombreRol(codigo) {
  return NOMBRES_ROL[codigo] || codigo;
}

export function nombreRolEmpresa(codigo) {
  return NOMBRES_ROL_EMPRESA[codigo] || codigo;
}
