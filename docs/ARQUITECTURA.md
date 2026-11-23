# Arquitectura

## 1. Vision general

PRAXIS es una aplicación web para gestionar las prácticas preprofesionales. Está construida con Java y Quarkus en el backend, React en el frontend y PostgreSQL como base de datos.

La arquitectura de referencia es:

```
Resource  -->  Service  -->  Repository  -->  Entity  -->  PostgreSQL
```

La base de datos PostgreSQL ya existe y se utilizará como referencia para crear las entidades. El backend debe adaptarse a sus tablas y columnas, sin generar ni modificar automáticamente su estructura.

## 2. Stack tecnologico

| Tecnología | Función | 
|---|---|
| Java 25 | Lenguaje del backend; versión oficial del proyecto. |
| Quarkus 3.40.1 | Framework del backend. |
| Quarkus REST | Recursos HTTP y JSON. |
| CDI (ArC) | Inyección de dependencias. |
| Hibernate ORM Panache | Acceso y persistencia de datos. |
| PostgreSQL 15+ | Base de datos relacional. |
| BCrypt | Hash de contraseñas. |
| Sesiones HTTP del servidor | Autenticación con cookie de sesión HttpOnly; sin JWT ni tokens. |
| Maven | Construcción y ejecución del backend. |
| React + Vite | Interfaz web. |

### Decisiones técnicas registradas

D1: Uso de Quarkus 3.40.1 con Java 25. El proyecto se compila y ejecuta con JDK 25 (LTS 25.0.2); no se usa otra versión de Java.
D2: Uso de Hibernate ORM Panache con patrón Repository.
D3: Inyección de dependencias mediante CDI.
D4: El backend expondrá endpoints REST que recibirán y devolverán JSON.
D5: Organización de paquetes por capa.
D6: Transacciones declaradas con `@Transactional` solo en los métodos de escritura del Service. 
D7: Autenticación mediante sesiones tradicionales del servidor Quarkus con cookie de sesión HttpOnly; las contraseñas se verifican con BCrypt. No se usa JWT, Bearer tokens, refresh tokens ni autenticación basada en tokens.

## 3. Estructura del proyecto

praxis/
├── src/
│   └── main/
│       ├── java/ec/edu/praxis/
│       │   ├── web/
│       │   │   └── resource/       Endpoints REST
│       │   ├── application/
│       │   │   └── service/        Operaciones y lógica del sistema
│       │   ├── domain/
│       │   │   └── model/          Entidades de la base de datos
│       │   └── infrastructure/
│       │       └── repository/     Acceso a PostgreSQL con Panache
│       ├── resources/
│       │   └── application.properties
│       └── webui/                  Frontend React + Vite
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

Reglas de organización:

- Una clase por archivo; el nombre del archivo coincide exactamente con el de la clase (ej. Estudiante.java, EstudianteRepository.java, EstudianteService.java, EstudianteResource.java).
- Los paquetes de capa son planos (sin subpaquetes por módulo).
- Se creará una clase por entidad, repositorio, servicio y recurso cuando el módulo lo necesite.

## 4. Arquitectura por capas

```
Navegador
    |
    v
React + Vite
    |
    | Solicitudes HTTP / JSON
    v
Resource
    |
    v
Service
    |
    v
Repository
    |
    v
Entity
    |
    v
PostgreSQL
```

## 5. Responsabilidades

| Capa | Responsabilidad |
|---|---|
| Resource | Define las rutas HTTP, recibe parámetros o datos JSON y llama al Service. Devuelve el resultado de la operación. |
| Service | Contiene las operaciones del sistema. Coordina repositorios cuando una funcionalidad necesita acceder a más de una entidad. |
| Repository | Extiende `PanacheRepositoryBase<Entidad, TipoId>` para heredar el CRUD y usa `EntityManager` con JPQL para las consultas específicas. |
| Entity | Representa una tabla de PostgreSQL mediante anotaciones JPA y contiene sus atributos, getters y setters. |

Identidad del actor y ámbito. El usuario autenticado siempre se obtiene de `SecurityIdentity` (`praxis.usuarioId`) en el Resource; el cliente no puede fijarlo con un `usuarioId` en ruta, consulta o cuerpo. El `empresaId` puede aparecer en la ruta o en el cuerpo cuando el contrato aprobado lo prevé (por ejemplo, API-043 a API-053 y el `empresaId` titular de un convenio), pero nunca se considera de confianza: el Service valida existencia, pertenencia y ámbito antes de operar. API-086 no acepta `empresaId` ni `usuarioId` del cliente y deriva el ámbito exclusivamente del actor autenticado.

Caso API-043 (convenios). `ConvenioResource.listarPorEmpresa()` no importa proyecciones del Repository: `ConvenioService` devuelve `ConvenioResumen` y el Resource publica esa misma estructura. La forma JSON no cambia; el Repository sigue siendo la única capa que consulta la vista `v_convenio_estado`.

Caso API-086 (Mi empresa). `EmpresaResource.miEmpresa()` es la única entrada HTTP: no recibe parámetros del cliente y toma `praxis.usuarioId` de `SecurityIdentity`. `EmpresaService.miEmpresa()` valida usuario activo y rol global REP, y coordina tres repositorios: `EmpresaUsuarioRepository` (proyección de solo lectura con la membresía `ADMIN_EMPRESA` efectiva, sin filtrar `empresa.activa`), `UsuarioRepository` y `UsuarioRolRepository`. Cero membresías dan 404 y más de una dan 409, sin selección arbitraria ni datos de otras empresas. El Repository devuelve una proyección interna con `empresaId` y `rolEmpresa`; el Service carga y devuelve la `Empresa`, y el Resource construye `MiEmpresaResponse` como única estructura de salida, fijando `rolEmpresa` en `ADMIN_EMPRESA` porque es la única membresía que autoriza la operación. No se agregaron entidad, DTO independiente, capa ni servicio genérico.

## 6. Inyeccion de dependencias

La inyección usa CDI de Quarkus (ArC):

- Service y Repository se declaran `@ApplicationScoped`.
- Se inyecta con @Inject sobre el campo, sin constructor.
- No se usan interfaces para inyectar: se inyecta la clase concreta.
- `EntityManager` se inyecta dentro del Repository para las consultas personalizadas con JPQL; no se inyecta en Resource ni en Service. Las operaciones CRUD se heredan de Panache y no se reimplementan.

```java
@ApplicationScoped
public class FacultadService {

    @Inject
    private FacultadRepository repository;
    
    @Transactional
    public Facultad crear(Facultad facultad) {
        repository.persist(facultad);
        return facultad;
    }

}
```

## 7. Persistencia

Entidades.

- Clase JPA simple (`@Entity`, `@Table(name = "...")`) con campos privados. No se extiende `PanacheEntity` ni `PanacheEntityBase`: se usa el patrón Repository.
- Cada columna se mapea con `@Column(name = "...")` explícito, respetando `snake_case` de la base.
- Clave primaria: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`. Las claves son `GENERATED ALWAYS`: el código nunca asigna un `id`.
- Claves compuestas (`usuario_rol`, `usuario_carrera`, `oferta_perfil_carrera`): `@EmbeddedId` o `@IdClass`, definido en cada módulo que las use.
- Claves foráneas: por defecto se mapean como columna `Long` (por ejemplo `facultadId`). Se usa `@ManyToOne(fetch = FetchType.LAZY)` solo cuando una consulta lo necesita.
- Columnas `version` (`practica`, `registro_horas`, `informe`, `postulacion`): se mapean con `@Version`. La concurrencia básica del MVP usa ese control optimista en `postulacion` y `practica`, comprueba la disponibilidad de plazas dentro de la transacción que acepta la postulación y traduce el conflicto a 409 `CONFLICTO_CONCURRENCIA`. No se agregan bloqueos distribuidos, colas ni mecanismos externos.
- Estados: la base usa `VARCHAR` con `CHECK`. En Java se usa `enum` con `@Enumerated(EnumType.STRING)` y nombres idénticos a los valores de la base.
- Columnas que llena la base o un trigger (created_at, updated_at, fecha_publicacion, *_snapshot, entre otras): no se mapean, salvo que haga falta mostrarlas; en ese caso se mapean con insertable = false, updatable = false.

Repository.

- Una clase `@ApplicationScoped` que implementa `PanacheRepositoryBase<Entidad, TipoId>` por cada entidad que lo necesite; `TipoId` declara explícitamente el tipo real de la clave primaria (`Long` en el modelo).
- Métodos con nombre de intención: `buscarPorCodigo`, `listarActivas`. Panache solo se usa dentro del Repository.
- Las consultas personalizadas se implementan con `EntityManager` y JPQL según §7.1.
- Las vistas `v_convenio_estado` y `v_oferta_perfil_cupo` son de solo lectura: no se mapean como entidades JPA. El Repository puede devolver proyecciones sencillas con los campos necesarios (`estadoEfectivo`; `aceptadas`, `disponibles`, `perfilCerrado`). No se crea una capa adicional ni una estructura compleja para ellas.

### 7.1. Convencion para consultas en repositorios

Patrón obligatorio para todo repositorio con consultas propias: heredar el CRUD de `PanacheRepositoryBase<Entidad, TipoId>` declarando explícitamente el tipo real del identificador, e inyectar `EntityManager` de Jakarta Persistence para las consultas personalizadas. Se usa JPQL explícito con `em.createQuery(...)`, consultas tipadas con `Entidad.class` y parámetros nombrados con `setParameter()`.

```java
@ApplicationScoped
public class EjemploRepository implements PanacheRepositoryBase<Ejemplo, Long> {

    @Inject
    EntityManager em;
}
```

A. Registro único. Usar `getSingleResult()` y capturar específicamente `NoResultException`; devolver `null` cuando no exista el registro, según la convención establecida. No capturar `Exception` ni tratar otros errores como "no encontrado".

```java
public Ejemplo buscarPorCodigo(String codigo) {
    try {
        return em.createQuery("SELECT e FROM Ejemplo e WHERE e.codigo = :codigo", Ejemplo.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    } catch (NoResultException e) {
        return null;
    }
}
```

B. Múltiples registros. Usar `getResultList()`; sin coincidencias devuelve una lista vacía. No agregar try-catch innecesarios.

```java
public List<Ejemplo> listarActivos() {
    return em.createQuery("SELECT e FROM Ejemplo e WHERE e.activa = :activa", Ejemplo.class)
            .setParameter("activa", true)
            .getResultList();
}
```

C. Consultas con fechas. Calcular los rangos con `java.time` en Java y enviar los valores calculados mediante parámetros JPQL. Evitar funciones específicas de PostgreSQL (`date_trunc`, `NOW() AT TIME ZONE`, entre otras) cuando JPQL pueda resolver la necesidad.

```java
public List<Ejemplo> listarHasta(OffsetDateTime fecha) {
    OffsetDateTime fin = fecha.plusDays(30);
    return em.createQuery("SELECT e FROM Ejemplo e WHERE e.fecha BETWEEN :inicio AND :fin", Ejemplo.class)
            .setParameter("inicio", fecha)
            .setParameter("fin", fin)
            .getResultList();
}
```

Reglas adicionales:

- No utilizar las abreviaturas de consultas de Panache (`find("campo", valor)`, `find("...", Sort, ...)`, etc.) para consultas personalizadas; las operaciones heredadas siguen disponibles sin reescribirse.
- No concatenar valores externos en el JPQL; siempre `setParameter()`.
- No usar consultas SQL nativas (`createNativeQuery`) sin una justificación técnica escrita en el Repository. En `EmpresaUsuarioRepository` las tres consultas nativas están justificadas: reproducen la lógica de la función `empresa_rol_activo()` o proyectan columnas de dos tablas sin relación JPA declarada. Son de solo lectura; no se reemplazan solo para eliminar SQL nativo.
- No reemplazar operaciones CRUD heredadas por implementaciones manuales innecesarias.
- Mantener los repositorios sencillos y legibles; no crear capas adicionales ni abstracciones innecesarias.

Transacciones.

- `@Transactional` (`jakarta.transaction.Transactional`) se declara en los métodos de escritura del Service.
- Las lecturas no abren transacción.
- No se declara `@Transactional` en Resource ni en Repository.
- Los errores de negocio se lanzan como excepciones no verificadas (`RuntimeException`); provocan el rollback de la transacción.

Comportamiento de la base que el código debe respetar:

- Los triggers pueden cambiar valores al guardar (estados, fechas, snapshots, `motivo_decision`). Si el Service necesita el valor final, lo relee después de `flush()`.
- Contraseñas se guardan con BCrypt: BcryptUtil.bcryptHash(clave) para crear el hash y BcryptUtil.matches(clave, hash) para compararlo (clase io.quarkus.elytron.security.common.BcryptUtil).
- El campo del hash en la entidad lleva @JsonIgnore.

## 8. Convenciones de codigo

| Aspecto | Convención |
|---|---|
| Idioma | Nombres del dominio en español; sufijos técnicos en inglés. |
| Paquete base | `ec.edu.praxis`. |
| Entity | Nombre en singular y en `PascalCase`, por ejemplo `Facultad`. |
| Repository | Nombre de la entidad seguido de `Repository`. |
| Service | Nombre de la entidad seguido de `Service`. |
| Resource | Nombre de la entidad seguido de `Resource`. |
| Atributos | `camelCase`. |
| Métodos | Nombres sencillos y descriptivos: `listar`, `buscar`, `crear`, `actualizar`, `eliminar`. |
| DTO | No se crean DTO independientes. Si hace falta una estructura de entrada, se declara dentro del Resource como clase interna. |
| Respuestas | Los repositorios trabajan con entidades JPA y los Services gestionan la lógica de negocio y devuelven los resultados necesarios. Los Resources construyen respuestas REST explícitas: las entidades JPA nunca se devuelven directamente al cliente, ni en operaciones individuales ni en listados, y tampoco se serializan proyecciones de vistas. No se requieren DTOs independientes ni mappers adicionales: las estructuras de salida se declaran como clases internas del Resource, según el patrón de `FacultadResource`. |
| Inyección | CDI mediante `@Inject`. |
| Transacciones | `@Transactional` en operaciones de escritura. |
| Consultas | CRUD heredado de `PanacheRepositoryBase<Entidad, TipoId>`; consultas propias con `EntityManager` y JPQL tipado según §7.1. |
| Contraseñas | Se utiliza BCrypt para generar y comprobar hashes. |

## 9. Configuracion local

### 9.1. Requisitos

| Requisito | Detalle |
|---|---|
| Java | JDK 25 (LTS 25.0.2); compilación y ejecución obligatorias con JDK 25. |
| Quarkus | 3.40.1 (definido en `pom.xml`). |
| Maven | El del wrapper del proyecto (`./mvnw`, `mvnw.cmd` en Windows); no requiere instalación. |
| Base de datos | PostgreSQL 15 o superior en el proyecto Supabase existente. La conexión utiliza Session Pooler, SSL y la configuración local externa de `config/local.properties`. Durante la configuración de conexión no se ejecutan migraciones ni se modifica el esquema o los datos. |
| Node.js | Necesario para trabajar con el frontend React/Vite. |

### 9.2. Conexión a la base de datos

`application.properties` solo declara el tipo de base; la URL, el usuario y la contraseña se cargan desde un archivo local no versionado:

```properties
quarkus.datasource.db-kind=postgresql
```

### 9.3. Configuración local

| Propiedad local | Contenido | Ejemplo no sensible |
|---|---|---|
| `quarkus.datasource.jdbc.url` | URL JDBC del Session Pooler. | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require` |
| `quarkus.datasource.username` | Usuario del proyecto actual. | `postgres.<referencia-del-proyecto>` |
| `quarkus.datasource.password` | Contraseña del proyecto actual. | (definida por cada computadora) |

La plantilla sin valores es `config/local.properties.example`. El archivo real `config/local.properties` está excluido por `.gitignore`. Quarkus exige una URI de archivo jerárquica, así que se carga con `-Dquarkus.config.locations` apuntando a la ruta absoluta calculada en el arranque. No se usan variables de entorno ni `.env` para esta conexión. El backend sigue siendo el único componente que accede a PostgreSQL; React solo consume REST.

### 9.4. Comandos

| Acción | Comando |
|---|---|
| Ejecutar en modo desarrollo (Windows) | `.\mvnw.cmd "-Dquarkus.config.locations=$(([Uri](Resolve-Path 'config\local.properties').Path).AbsoluteUri)" quarkus:dev` |
| Ejecutar pruebas | `./mvnw test` |
| Construir | `./mvnw package` |

## 10. Modulo de referencia

Este módulo es el patrón que deben seguir todos los demás. Implementa el CRUD de facultades, parte de la estructura académica del MVP.

Las rutas REST se declaran relativas a la raíz, sin prefijo `/api` (por ejemplo, `@Path("/facultades")`). Este es el prefijo oficial de la API (CR-1).

Tabla elegida: `facultad` (`MODELO_DATOS.md`). Es la más simple del modelo: no tiene claves foráneas y sus restricciones son una clave única en `codigo` y otra en `nombre`.

| Columna | Tipo | Nulo | Clave | Nota |
|---|---|---|---|---|
| id | BIGINT | No | PK | `GENERATED ALWAYS AS IDENTITY`; se mapea como `Long` en Java. |
| codigo | VARCHAR(20) | No | UK | |
| nombre | VARCHAR(150) | No | UK | |
| activa | BOOLEAN | No | – | Def: TRUE. |
| created_at | TIMESTAMPTZ | No | – | Def: NOW(). |
| updated_at | TIMESTAMPTZ | No | – | Def: NOW(); lo actualiza el trigger `set_updated_at`. |

### 10.1. Archivos del módulo

```
src/main/java/ec/edu/praxis/
├── domain/model/Facultad.java
├── infrastructure/repository/FacultadRepository.java
├── application/service/FacultadService.java
└── web/resource/FacultadResource.java
```

### 10.2. Entity

```java
package ec.edu.praxis.domain.model;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "facultad")
public class Facultad {

    // GENERATED ALWAYS: la base asigna el id; el código nunca lo establece.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    // Columnas gestionadas por la base (DEFAULT NOW() y trigger set_updated_at).
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = { EventType.INSERT, EventType.UPDATE })
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
	 return id; 
    }

    public String getCodigo() {
	 return codigo; 
    }

    public void setCodigo(String codigo) {
 	this.codigo = codigo; 
    }

    public String getNombre() {
	return nombre; 
    }
    public void setNombre(String nombre) {
	this.nombre = nombre; 
    }

    public boolean isActiva() { 
	return activa; 
    }
    public void setActiva(boolean activa) { 
	this.activa = activa; 
    }

    public OffsetDateTime getCreatedAt() { 
	return createdAt; 
    }
    public OffsetDateTime getUpdatedAt() {
	return updatedAt; 
    }
}
```

### 10.3. Repository

```java
package ec.edu.praxis.infrastructure.repository;

import ec.edu.praxis.domain.model.Facultad;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class FacultadRepository implements PanacheRepositoryBase<Facultad, Long> {

    @Inject
    EntityManager em;

    public Facultad buscarPorCodigo(String codigo) {
        try {
            return em.createQuery("SELECT f FROM Facultad f WHERE f.codigo = :codigo", Facultad.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Facultad buscarPorNombre(String nombre) {
        try {
            return em.createQuery("SELECT f FROM Facultad f WHERE f.nombre = :nombre", Facultad.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
```

`findById`, `listAll`, `persist`, `flush` y `deleteById` los aporta `PanacheRepositoryBase`; no se reescriben. Las dos consultas propias siguen el patrón A de §7.1.

### 10.4. Service

```java
package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Facultad;
import ec.edu.praxis.infrastructure.repository.FacultadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FacultadService {
    
    @Inject
    private FacultadRepository repository;

    public List<Facultad> listar() {
        return repository.listAll();
    }

    public Facultad buscar(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Facultad crear(Facultad facultad) {
        repository.persist(facultad);
        return facultad;
    }

    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public Facultad actualizar(Facultad datos) {
        Facultad facultad = repository.findById(datos.getId());
        if (facultad == null) {
            throw new IllegalArgumentException("Facultad no encontrada: " + datos.getId());
        }
        facultad.setCodigo(datos.getCodigo());
        facultad.setNombre(datos.getNombre());
        facultad.setActiva(datos.isActiva());
        return facultad;
    }

```

### 10.5. Resource JSON

```java
package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.FacultadService;
import ec.edu.praxis.domain.model.Facultad;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/facultades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacultadResource {

    @Inject
    FacultadService service;

    @GET
    @Path("/listarTodos")
    public Response listar() {
        List<FacultadResponse> respuesta = service.listar().stream()
                .map(FacultadResponse::new)
                .toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Response.ok(new FacultadResponse(service.buscar(id))).build();
    }

    @POST
    public Response crear(Facultad facultad) {
        return Response.status(Response.Status.CREATED)
                .entity(new FacultadResponse(service.crear(facultad)))
                .build();
    }

    @PUT
    public Response actualizar(Facultad facultad) {
        return Response.ok(new FacultadResponse(service.actualizar(facultad))).build();
    }

    @DELETE
    @Path("/eliminar/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        service.eliminar(id);
        return Response.noContent().build();
    }

    /** Estructura de salida: no se devuelve la entidad JPA directamente. */
    public static class FacultadResponse {

        private final Long id;
        private final String codigo;
        private final String nombre;
        private final boolean activa;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        FacultadResponse(Facultad facultad) {
            this.id = facultad.getId();
            this.codigo = facultad.getCodigo();
            this.nombre = facultad.getNombre();
            this.activa = facultad.isActiva();
            this.createdAt = facultad.getCreatedAt();
            this.updatedAt = facultad.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getNombre() {
            return nombre;
        }

        public boolean isActiva() {
            return activa;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
```

El Resource construye las respuestas con la clase interna `FacultadResponse`: no serializa la entidad JPA ni requiere DTOs independientes, mappers o capas adicionales (§8, §12.2).

El endpoint `DELETE` se conserva en el contrato REST (API-009) como patrón de referencia. La interfaz no ofrece eliminar facultades: solo desactivarlas (API-008). La eliminación debe respetar la integridad referencial de la base; si existen carreras u otros registros relacionados, la operación responde 409. No se asume que una facultad con registros asociados pueda borrarse físicamente.

## 11. Autenticación y sesión

Decisión técnica oficial (CR-2): PRAXIS usa **sesiones tradicionales del servidor Quarkus** con **cookie de sesión HttpOnly**. La contraseña almacenada se verifica con **BCrypt**. No se usan JWT, Bearer tokens, refresh tokens ni autenticación basada en tokens. No se crea ninguna tabla nueva para gestionar sesiones.

Flujo:

1. El usuario envía su correo y contraseña a `POST /auth/login`.
2. Quarkus verifica que la cuenta esté activa y que las credenciales sean válidas; la contraseña se compara con el hash BCrypt.
3. Si son correctas, el servidor crea la sesión y devuelve la cookie de sesión.
4. El navegador conserva la cookie y la envía en las siguientes peticiones.
5. Quarkus comprueba la sesión y los permisos del usuario en cada petición.
6. `POST /auth/logout` invalida la sesión en el servidor.

Mecanismo de autenticación e identidad por petición (decisión técnica aprobada):

- `HttpAuthenticationMechanism` se implementa en **una única clase adaptadora**; las importaciones internas `io.quarkus.vertx.http.runtime.security.*` quedan confinadas a esa clase. No se agrega ninguna dependencia.
- El adaptador devuelve desde `authenticate(RoutingContext, IdentityProviderManager)` un `Uni<SecurityIdentity>` construido con la **interfaz pública** `io.quarkus.security.identity.SecurityIdentity`. No se usa `QuarkusSecurityIdentity` porque no está en el classpath efectivo del proyecto.
- La implementación de `SecurityIdentity` es una **clase interna del adaptador**. Por cada petición autenticada se crea una **instancia nueva**, con solo el identificador del usuario, su correo y sus roles (`getPrincipal`, `isAnonymous` en `false`, `getRoles`, `hasRole`). No se guarda en un campo de la clase adaptadora ni se reutiliza entre usuarios.
- Quarkus asocia la identidad devuelta al `RoutingContext` y la pone a disposición del resto de la petición; por eso **no se llama `CurrentIdentityAssociation.setIdentity(...)` desde el adaptador**. Durante la implementación se verifica que `CurrentIdentityAssociation.current()` devuelve la identidad de la petición.
- Estas firmas se validaron contra los JAR efectivos de Quarkus 3.40.1 y se revalidan en cada actualización de la versión.

Medidas básicas de la cookie:

- La cookie de sesión es `HttpOnly` (no accesible desde JavaScript), con `Secure` en despliegues HTTPS y `SameSite` apropiado para el frontend React; no contiene datos del usuario ni credenciales.
- La protección CSRF no forma parte del MVP; se considera una mejora futura de seguridad. No se instalará `quarkus-rest-csrf` ni se implementará `CSRFHandler` u otro mecanismo propio durante el MVP. El origen del frontend se restringe con CORS y se rechazan orígenes desconocidos.
- La sesión se invalida al cerrar sesión; una sesión ausente, vencida o inválida responde 401.
- La asignación o modificación de roles (`asignarRoles`), la desactivación (`desactivar`) y el restablecimiento de contraseña (`restablecerPassword`) invalidan todas las sesiones del usuario afectado. El Service publica un evento simple de capa de aplicación dentro de su transacción; un observador `@Observes(during = TransactionPhase.AFTER_SUCCESS)` invoca `SesionStore.invalidarPorUsuario` después del commit. Ante error o rollback no se ejecuta la invalidación. No se consulta PostgreSQL en cada petición ni se introducen tokens ni almacenamiento externo.
- `SesionStore` mantiene una generación por usuario en un `ConcurrentHashMap` en memoria. `AutenticacionService.login` obtiene `generacionActual(usuarioId)` antes de consultar los roles y la pasa a `crear`; si la generación ya cambió por una invalidación confirmada, `crear` devuelve `null` y el login responde con un fallo genérico. La comprobación de la generación y la inserción de la sesión se ejecutan bajo el mismo cerrojo que `invalidarPorUsuario` (que incrementa la generación y elimina las sesiones), sin mantener bloqueos durante consultas a PostgreSQL. `buscar` vuelve a verificar la generación y descarta sesiones obsoletas. El mapa de generaciones no se limpia: su tamaño está acotado por el número de usuarios del sistema; rastrear logins en vuelo para limpiarlo añadiría complejidad desproporcionada al MVP. Los resultados de pruebas, las limitaciones pendientes y el dictamen de la auditoría H-01 están consolidados en `TAREAS.md` §6.3.2; el dictamen es **CORREGIDO CON LIMITACIONES DOCUMENTADAS**.
- El identificador de sesión se guarda en un almacén sencillo en memoria del servidor, gestionado por una sola clase; la cookie solo transporta ese identificador aleatorio.
- La expiración de la sesión es fija y configurable; no hay renovación automática en el MVP.
- Si una cuenta se bloquea después de haber iniciado sesión, su sesión activa conserva su vigencia hasta expirar o hasta el cierre de sesión; el bloqueo impide iniciar nuevas sesiones. No se consulta PostgreSQL en cada petición para detectar bloqueos. Esta limitación comprende solo el bloqueo por intentos fallidos y es independiente de la invalidación por cambio de roles, desactivación o restablecimiento de contraseña descrita arriba.
- Por tratarse de un almacén en memoria, las sesiones se pierden al reiniciar el servidor y el diseño corresponde a una sola instancia; no hay sesiones distribuidas.
- Duración de la sesión, duración de la cookie (`Max-Age`), nombre de la cookie y `SameSite` son parámetros configurables sin valor aprobado; no se fijan en el código (`SEGURIDAD.md` §2.8).
- Cualquier extensión o dependencia adicional necesaria se incorpora solo con aprobación (§12.8); el diseño de la autenticación no depende de tokens.

## 12. Restricciones tecnicas

1. Simplicidad: no se agregan capas, interfaces, patrones o abstracciones que no sean necesarios.
2. Sin DTO independientes: las entidades JPA se utilizan internamente; los Resources construyen respuestas REST explícitas como clases internas y no devuelven entidades JPA directamente al cliente. No se añaden capas de transformación ni DTOs independientes innecesarios, en coherencia con §8.
3. Respeto a los triggers. No se escriben directamente las columnas gestionadas por triggers (snapshots, fechas de publicación y resolución de ofertas). Se debe prever que los valores guardados difieran de los enviados, que algunos errores aparezcan al confirmar la transacción y que ciertas operaciones requieran el contexto de sesión praxis.
4. Parámetros pendientes de aprobación. No se fijan valores rígidos para horas requeridas, nota mínima, ponderaciones, plazos de vigencia ni requisitos académicos: se gestionan como datos o configuración. Los límites ya fijados en la base (8 horas diarias, 2 MB por informe) se respetan tal cual.
5. Zona horaria. La zona de negocio es `America/Guayaquil`. Las fechas con hora que se muestran al usuario se convierten a esa zona; las columnas `TIMESTAMPTZ` se leen como `OffsetDateTime`.
6. Alcance del MVP. No se implementan módulos excluidos (actividades y horas, informes, evaluaciones, certificados, reportes avanzados). Tampoco se implementa la transición de la práctica a `EN_CURSO` (API-083), la paginación de listados ni la definición automatizada de requisitos académicos como módulo; las validaciones académicas de la postulación sí se aplican en la API. Estas capacidades quedan identificadas como mejoras futuras.
7. Secretos. Credenciales, cadenas con contraseña y llaves privadas no se versionan; se leen del entorno.
8. Dependencias. Agregar una dependencia o extensión nueva al `pom.xml` requiere aprobación del equipo.
