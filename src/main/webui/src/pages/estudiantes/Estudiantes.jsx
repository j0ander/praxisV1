import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Tabla from '../../components/comunes/Tabla';
import { mensajeDeError } from '../../services/api';
import { listarCarreras } from '../../services/estructuraService';
import {
  buscarEstudiante,
  crearEstudiante,
  actualizarEstudiante,
  listarEstudiantes,
} from '../../services/perfilService';
import { listarUsuarios } from '../../services/usuarioService';

const PERFIL_VACIO = {
  usuarioId: '',
  carreraId: '',
  codigoEstudiante: '',
  fechaNacimiento: '',
};

export default function Estudiantes() {
  const [carreras, setCarreras] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [cargandoUsuarios, setCargandoUsuarios] = useState(false);
  const [errorUsuarios, setErrorUsuarios] = useState(null);
  const [filtroCarrera, setFiltroCarrera] = useState('');
  const [estudiantes, setEstudiantes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [perfil, setPerfil] = useState(null);
  const [formulario, setFormulario] = useState(PERFIL_VACIO);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    listarCarreras()
      .then((datos) => setCarreras(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)));
  }, []);

  const cargarUsuarios = useCallback(() => {
    setCargandoUsuarios(true);
    setErrorUsuarios(null);
    listarUsuarios()
      .then((datos) => setUsuarios(Array.isArray(datos) ? datos : []))
      .catch((fallo) => {
        setUsuarios([]);
        setErrorUsuarios(mensajeDeError(fallo));
      })
      .finally(() => setCargandoUsuarios(false));
  }, []);

  useEffect(() => {
    cargarUsuarios();
  }, [cargarUsuarios]);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarEstudiantes(filtroCarrera)
      .then((datos) => setEstudiantes(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, [filtroCarrera]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const carrerasActivas = carreras.filter((carrera) => carrera.activa !== false);

  function nombreCarrera(id) {
    const carrera = carreras.find((item) => String(item.id) === String(id));
    return carrera ? `${carrera.nombre} (${carrera.codigo})` : `ID ${id}`;
  }

  async function abrirPerfil(item) {
    setError(null);
    setResultado(null);
    try {
      const datos = item?.id ? await buscarEstudiante(item.id) : item;
      setPerfil(datos || {});
      setFormulario({
        usuarioId: datos?.usuarioId ?? '',
        carreraId: datos?.carreraId ?? '',
        codigoEstudiante: datos?.codigoEstudiante ?? '',
        fechaNacimiento: datos?.fechaNacimiento ?? '',
      });
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    }
  }

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  async function guardar(evento) {
    evento.preventDefault();
    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      const datos = {
        carreraId: Number(formulario.carreraId),
        codigoEstudiante: formulario.codigoEstudiante.trim() || null,
        fechaNacimiento: formulario.fechaNacimiento || null,
      };
      if (perfil?.id) {
        datos.id = perfil.id;
        datos.usuarioId = perfil.usuarioId;
        await actualizarEstudiante(datos);
        setResultado({ tipo: 'exito', mensaje: 'Perfil actualizado.' });
      } else {
        datos.usuarioId = Number(formulario.usuarioId);
        await crearEstudiante(datos);
        setResultado({ tipo: 'exito', mensaje: 'Perfil creado.' });
      }
      setPerfil(null);
      setFormulario(PERFIL_VACIO);
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  const columnas = [
    {
      clave: 'codigoEstudiante',
      titulo: 'Código',
      render: (item) => item.codigoEstudiante || '—',
    },
    {
      clave: 'carreraId',
      titulo: 'Carrera',
      render: (item) => nombreCarrera(item.carreraId),
    },
    {
      clave: 'fechaNacimiento',
      titulo: 'Fecha de nacimiento',
      render: (item) => item.fechaNacimiento || '—',
    },
    {
      clave: 'usuarioId',
      titulo: 'Usuario',
      render: (item) => item.usuarioId,
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (item) => (
        <Boton variante="enlace" pequeno onClick={() => abrirPerfil(item)}>
          Ver / Editar
        </Boton>
      ),
    },
  ];

  const esEdicion = Boolean(perfil?.id);

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Estudiantes</h1>
          <p>Gestión de perfiles académicos de estudiantes.</p>
        </div>
        <Boton onClick={() => abrirPerfil(null)}>Nuevo perfil</Boton>
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      {!perfil ? <MensajeEstado tipo="error" mensaje={error} onReintentar={cargar} /> : null}

      {perfil ? (
        <section className="tarjeta">
          <h2>{esEdicion ? 'Editar perfil' : 'Nuevo perfil de estudiante'}</h2>
          <form onSubmit={guardar}>
            <div className="rejilla">
              {esEdicion ? (
                <CampoFormulario id="usuario-perfil" etiqueta="Usuario" value={perfil.usuarioId} disabled readOnly />
              ) : (
                <CampoFormulario id="usuario-perfil" etiqueta="Usuario" obligatorio>
                  <select
                    id="usuario-perfil"
                    value={formulario.usuarioId}
                    onChange={(evento) => cambiarCampo('usuarioId', evento.target.value)}
                    required
                  >
                    <option value="">Seleccione un usuario</option>
                    {usuarios.map((usuario) => (
                      <option key={usuario.id} value={usuario.id}>
                        {usuario.nombres} {usuario.apellidos} · {usuario.correo}
                      </option>
                    ))}
                  </select>
                  {cargandoUsuarios ? <small>Cargando usuarios…</small> : null}
                  {!cargandoUsuarios && errorUsuarios ? (
                    <MensajeEstado
                      tipo="error"
                      mensaje={errorUsuarios}
                      onReintentar={cargarUsuarios}
                    />
                  ) : null}
                  {!cargandoUsuarios && !errorUsuarios && usuarios.length === 0 ? (
                    <small>No hay usuarios disponibles para asignar.</small>
                  ) : null}
                </CampoFormulario>
              )}
              <CampoFormulario id="carrera-perfil" etiqueta="Carrera" obligatorio>
                <select
                  id="carrera-perfil"
                  value={formulario.carreraId}
                  onChange={(evento) => cambiarCampo('carreraId', evento.target.value)}
                  required
                >
                  <option value="">Seleccione una carrera</option>
                  {carrerasActivas.map((carrera) => (
                    <option key={carrera.id} value={carrera.id}>
                      {carrera.nombre} ({carrera.codigo})
                    </option>
                  ))}
                </select>
                {carrerasActivas.length === 0 ? (
                  <small>No hay carreras activas disponibles.</small>
                ) : null}
              </CampoFormulario>
              <CampoFormulario
                id="codigo-estudiante"
                etiqueta="Código de estudiante"
                value={formulario.codigoEstudiante}
                onChange={(evento) => cambiarCampo('codigoEstudiante', evento.target.value)}
              />
              <CampoFormulario
                id="fecha-nacimiento"
                etiqueta="Fecha de nacimiento"
                type="date"
                value={formulario.fechaNacimiento}
                onChange={(evento) => cambiarCampo('fechaNacimiento', evento.target.value)}
              />
            </div>
            <MensajeEstado tipo="error" mensaje={error} />
            <div className="formulario__acciones">
              <Boton
                type="submit"
                cargando={guardando}
                disabled={!esEdicion && (Boolean(errorUsuarios) || usuarios.length === 0)}
              >
                {esEdicion ? 'Guardar cambios' : 'Crear perfil'}
              </Boton>
              <Boton variante="secundario" onClick={() => setPerfil(null)} disabled={guardando}>
                Cancelar
              </Boton>
            </div>
          </form>
        </section>
      ) : null}

      <section className="tarjeta">
        <h2>Búsqueda</h2>
        <div className="rejilla">
          <CampoFormulario id="filtro-carrera" etiqueta="Carrera">
            <select
              id="filtro-carrera"
              value={filtroCarrera}
              onChange={(evento) => setFiltroCarrera(evento.target.value)}
            >
              <option value="">Todas las carreras</option>
              {carreras.map((carrera) => (
                <option key={carrera.id} value={carrera.id}>
                  {carrera.nombre} ({carrera.codigo})
                </option>
              ))}
            </select>
          </CampoFormulario>
        </div>
      </section>

      <section className="tarjeta">
        <h2>Perfiles registrados</h2>
        <Tabla
          columnas={columnas}
          filas={estudiantes}
          cargando={cargando}
          error={null}
          vacio="No se encontraron perfiles de estudiante."
        />
      </section>
    </>
  );
}
