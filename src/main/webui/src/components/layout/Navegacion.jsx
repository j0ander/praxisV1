import React from 'react';

import { useSesion } from '../../contextos/SesionContexto';
import { usarRuta } from '../../routes/navegacion';
import RutaEnlace from '../comunes/RutaEnlace';

const OPCIONES = [
  { ruta: '/inicio', etiqueta: 'Inicio' },
  { ruta: '/admin/usuarios', etiqueta: 'Usuarios', roles: ['ADMINISTRADOR'] },
  { ruta: '/admin/facultades', etiqueta: 'Facultades' },
  { ruta: '/admin/carreras', etiqueta: 'Carreras' },
  { ruta: '/admin/periodos', etiqueta: 'Periodos' },
  { ruta: '/admin/tipos-practica', etiqueta: 'Tipos de práctica' },
  { ruta: '/estudiantes', etiqueta: 'Estudiantes', roles: ['ADMINISTRADOR', 'COORDINADOR'] },
  { ruta: '/docentes', etiqueta: 'Docentes', roles: ['ADMINISTRADOR', 'COORDINADOR'] },
  { ruta: '/empresas', etiqueta: 'Empresas', roles: ['ADMINISTRADOR', 'COORDINADOR'] },
  { ruta: '/mi-empresa', etiqueta: 'Mi empresa', roles: ['REPRESENTANTE_EMPRESA'], miEmpresa: true },
];

/**
 * La opción «Mi empresa» exige el rol global y la validación ADMIN_EMPRESA que
 * API-086 realizó en AppRoutes; no se muestra mientras la comprobación está en
 * curso ni se concede por la mera presencia del rol.
 */
export default function Navegacion({ accesoMiEmpresa = false }) {
  const { tieneRol } = useSesion();
  const ruta = usarRuta();

  const opciones = OPCIONES.filter((opcion) => {
    if (opcion.roles && !tieneRol(...opcion.roles)) return false;
    if (opcion.miEmpresa) return accesoMiEmpresa;
    return true;
  });

  return (
    <nav className="navegacion" aria-label="Navegación principal">
      <ul className="navegacion__lista">
        {opciones.map((opcion) => {
          const activa = ruta === opcion.ruta || (opcion.ruta !== '/inicio' && ruta.startsWith(`${opcion.ruta}/`));
          return (
            <li key={opcion.ruta}>
              <RutaEnlace
                a={opcion.ruta}
                className={`navegacion__enlace${activa ? ' navegacion__enlace--activo' : ''}`}
                aria-current={activa ? 'page' : undefined}
              >
                {opcion.etiqueta}
              </RutaEnlace>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
