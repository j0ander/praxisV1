
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
    roles: ['ADMINISTRADOR', 'COORDINADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/admin/carreras',
    etiqueta: 'Carreras',
    detalle: 'Carreras de cada facultad.',
    roles: ['ADMINISTRADOR', 'COORDINADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/admin/periodos',
    etiqueta: 'Periodos',
    detalle: 'Periodos académicos.',
    roles: ['ADMINISTRADOR', 'COORDINADOR', 'AUTORIDAD'],
  },
  {
    ruta: '/admin/tipos-practica',
    etiqueta: 'Tipos de práctica',
    detalle: 'Tipos de práctica preprofesional.',
    roles: ['ADMINISTRADOR', 'COORDINADOR', 'AUTORIDAD'],
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
    roles: ['ADMINISTRADOR', 'COORDINADOR'],
  },
  {
    ruta: '/empresas',
    etiqueta: 'Empresas',
    detalle: 'Empresas, miembros, convenios y tutores.',
    roles: [
      'ADMINISTRADOR',
      'COORDINADOR',
      'REPRESENTANTE_EMPRESA',
      'AUTORIDAD',
    ],
  },
];

function tieneRol(roles, permitidos) {
  return Array.isArray(roles) && roles.some((rol) => permitidos.includes(rol));
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
