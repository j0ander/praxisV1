# PRAXIS WebUI

Interfaces React/Vite implementadas para usuarios, perfiles académicos y empresas (TK-017 y TK-020, con pantallas de TK-018 y TK-019), con un login básico de prototipo. No hay sesión del servidor ni autorización real: el menú y las rutas se filtran por rol solo de forma visual (`SEGURIDAD.md`, `INTERFACES.md` §2).

## Requisitos

- Node.js 20.19+ (build ejecutado con la versión instalada en el entorno).
- Backend Quarkus en `http://127.0.0.1:8080` (o la URL definida en `PRAXIS_API`).

## Comandos

```bash
npm install
npm run dev      # http://127.0.0.1:5173
npm run build    # genera dist/
npm run preview  # http://127.0.0.1:4173
```

El servidor de desarrollo y `preview` proxyan las rutas REST (`/usuarios`, `/estudiantes`, `/docentes`, `/facultades`, `/carreras`, `/periodos`, `/tiposPractica`, `/empresas`, `/convenios`) al backend para evitar problemas de origen entre frontend y API, incluido `POST /usuarios/login`. No hay cookie de sesión.

Para apuntar a otro backend:

```powershell
$env:PRAXIS_API = 'http://127.0.0.1:8080'
npm run dev
```

No hay sesión que recuperar: no se usa `GET /auth/yo`, `localStorage` ni `sessionStorage`. El usuario y sus roles viven solo en `useState` en `App.jsx`; al recargar vuelve a pedirse el login.

## Pantallas y rutas implementadas

| Ruta | Pantalla | Roles con enlace visible (solo visual) |
|---|---|---|
| `/login` | Inicio de sesión | – |
| `/inicio` | Panel de inicio | Todos los usuarios con login válido |
| `/admin/usuarios`, `/admin/usuarios/nuevo`, `/admin/usuarios/:id` | Usuarios y formulario | `ADMINISTRADOR` |
| `/admin/facultades` | Facultades | `ADMINISTRADOR`, `COORDINADOR`, `AUTORIDAD` |
| `/admin/carreras` | Carreras | `ADMINISTRADOR`, `COORDINADOR`, `AUTORIDAD` |
| `/admin/periodos` | Períodos académicos | `ADMINISTRADOR`, `COORDINADOR`, `AUTORIDAD` |
| `/admin/tipos-practica` | Tipos de práctica | `ADMINISTRADOR`, `COORDINADOR`, `AUTORIDAD` |
| `/estudiantes` | Listado de estudiantes | `ADMINISTRADOR`, `COORDINADOR` |
| `/docentes` | Listado de docentes | `ADMINISTRADOR`, `COORDINADOR` |
| `/empresas`, `/empresas/:id` | Empresas (UI-010, UI-011) | `ADMINISTRADOR`, `COORDINADOR`, `TUTOR_EMPRESARIAL`, `REPRESENTANTE_EMPRESA`, `AUTORIDAD` |

El detalle de empresa incluye cuatro pestañas: datos, convenios, miembros y tutores empresariales. La columna «Rol» de la tabla anterior indica solo qué enlaces se muestran en el prototipo: las rutas se redirigen en React, pero la API no valida roles. `/cambiar-password` y `/mi-empresa` siguen pendientes a futuro (`SEGURIDAD.md`, `INTERFACES.md`).

## No implementado

No existen `/cambiar-password` (UI-002) ni `/mi-empresa` (API-086): quedan pendientes a futuro. UI-001 está vigente solo como login básico contra `POST /usuarios/login`; `debeCambiarPassword` se conserva en la base pero no redirige ni bloquea rutas. Las pantallas de ofertas, postulaciones, prácticas y auditoría no están implementadas, por lo que no se muestran en el menú aunque el rol aparezca en los datos de dominio.

La validación histórica incluyó `npm run build`, comprobación de las rutas REST a través del proxy y revisión del layout en escritorio (1280 px), tablet (768 px) y móvil (390 px), incluidos formularios y listas desplegables. Los desplegables de miembros y tutores muestran solo usuarios activos con el rol correspondiente que aún no estén vinculados. El login básico se verificó además en navegador: credenciales correctas e incorrectas, menú por rol, rutas ajenas, recarga y cierre de sesión.
