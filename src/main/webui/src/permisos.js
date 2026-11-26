
export const MODULOS = [
  {
    ruta: '/admin/usuarios',
    etiqueta: 'Usuarios',
    detalle: 'Cuentas, roles y carreras.',
    roles: ['ADMINISTRADOR'],
  },
  {
    ruta: '/admin/facultades',
    etiqueta: 'Facultades',
    detalle: 'Estructura académica.',
    roles: ['ADMINISTRADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/admin/carreras',
    etiqueta: 'Carreras',
    detalle: 'Carreras de cada facultad.',
    roles: ['ADMINISTRADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/admin/periodos',
    etiqueta: 'Periodos',
    detalle: 'Periodos académicos.',
    roles: ['ADMINISTRADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/admin/tipos-practica',
    etiqueta: 'Tipos de práctica',
    detalle: 'Tipos de práctica preprofesional.',
    roles: ['ADMINISTRADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/estudiantes',
    etiqueta: 'Estudiantes',
    detalle: 'Perfiles académicos de estudiantes.',
    roles: ['ADMINISTRADOR', 'COORDINADOR'],
  },
  {
    ruta: '/docentes',
    etiqueta: 'Docentes',
    detalle: 'Perfiles académicos de docentes.',
    roles: ['ADMINISTRADOR'],
  },
  {
    ruta: '/empresas',
    etiqueta: 'Empresas',
    detalle: 'Empresas, miembros, convenios y tutores.',
    roles: [
      'ADMINISTRADOR',
      'REPRESENTANTE_EMPRESA',
      'AUTORIDAD',
    ],
  },
];

function tieneRol(roles, permitidos) {
  return Array.isArray(roles) && roles.some((rol) => permitidos.includes(rol));
}

const ROLES_CON_ACCESO_TOTAL_EMPRESAS = ['ADMINISTRADOR', 'AUTORIDAD'];

export function puedeCrearEmpresa(roles) {
  return tieneRol(roles, ROLES_CON_ACCESO_TOTAL_EMPRESAS);
}

export function esRepresentanteRestringido(roles) {
  return tieneRol(roles, ['REPRESENTANTE_EMPRESA'])
    && !tieneRol(roles, ROLES_CON_ACCESO_TOTAL_EMPRESAS);
}

export function esCoordinador(roles) {
  return tieneRol(roles, ['COORDINADOR'])
    && !tieneRol(roles, ['ADMINISTRADOR', 'AUTORIDAD']);
}

export function modulosPermitidos(roles) {
  return MODULOS.filter((modulo) => tieneRol(roles, modulo.roles));
}

export function primeraRutaPermitida(roles) {
  return modulosPermitidos(roles)[0]?.ruta ?? '/inicio';
}

export function rutaPermitida(ruta, roles) {
  if (!ruta || ruta === '/' || ruta === '' || ruta === '/login' || ruta === '/inicio') {
    return true;
  }
  const modulo = MODULOS.find(
    (item) => ruta === item.ruta || ruta.startsWith(`${item.ruta}/`),
  );
  return modulo ? tieneRol(roles, modulo.roles) : false;
}
