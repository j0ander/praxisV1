# PRAXIS WebUI

Interfaces React/Vite implementadas para acceso, usuarios, perfiles académicos y empresas (TK-017 y TK-020, con pantallas de TK-018 y TK-019).

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

El servidor de desarrollo y `preview` proxyan las rutas REST (`/auth`, `/usuarios`, `/estudiantes`, `/docentes`, `/facultades`, `/carreras`, `/periodos`, `/tiposPractica`, `/empresas`, `/convenios`) al backend para conservar la cookie de sesión `HttpOnly` en el mismo origen aparente.

Para apuntar a otro backend:

```powershell
$env:PRAXIS_API = 'http://127.0.0.1:8080'
npm run dev
```

No se usa `localStorage` ni `sessionStorage`: la sesión se recupera con `GET /auth/yo`.

## Pantallas y rutas implementadas

| Ruta | Pantalla | Acceso |
|---|---|---|
| `/login` | Inicio de sesión | Público |
| `/inicio` | Panel de inicio | Autenticado |
| `/admin/usuarios`, `/admin/usuarios/nuevo`, `/admin/usuarios/:id` | Usuarios y formulario | `ADMINISTRADOR` |
| `/estudiantes` | Listado de estudiantes | `ADMINISTRADOR`, `COORDINADOR` |
| `/docentes` | Listado de docentes | `ADMINISTRADOR`, `COORDINADOR` |
| `/empresas` | Listado de empresas (UI-010) | `ADMINISTRADOR`, `COORDINADOR` |
| `/empresas/:id` | Detalle de empresa (UI-011, contexto institucional) | `ADMINISTRADOR`, `COORDINADOR` |
| `/mi-empresa` | Detalle de la propia empresa (UI-011), con `empresaId` obtenido de API-086 | `REPRESENTANTE_EMPRESA` con membresía activa `ADMIN_EMPRESA` validada por el backend |

El detalle de empresa incluye cuatro pestañas: datos, convenios, miembros y tutores empresariales. Los permisos por operación se derivan del contexto de navegación; la autorización efectiva siempre la decide la API. La opción «Mi empresa» aparece solo cuando API-086 responde correctamente, y una sola consulta alimenta el menú y el contenedor.

## No implementado

No existe la pantalla `/cambiar-password` (UI-002): depende de API-004, pendiente. `debeCambiarPassword` se registra en el backend pero no redirige ni bloquea rutas. Las pantallas de ofertas, postulaciones, prácticas y auditoría no están implementadas.

La validación consistió en `npm run build` y revisión estática de rutas; no se realizaron pruebas manuales de navegador.
