import React, { useCallback, useEffect, useRef, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Modal from '../../components/comunes/Modal';
import Tabla from '../../components/comunes/Tabla';
import { useSesion } from '../../contextos/SesionContexto';
import { mensajeDeError } from '../../services/api';
import {
  actualizarCarrera,
  crearCarrera,
  listarCarreras,
  listarFacultades,
} from '../../services/estructuraService';

const FORMULARIO_VACIO = { codigo: '', nombre: '', facultadId: '' };

function validarFormulario(formulario) {
  const errores = {};
  const codigo = formulario.codigo.trim();
  const nombre = formulario.nombre.trim();

  if (!codigo) {
    errores.codigo = 'El código es obligatorio.';
  } else if (codigo.length > 20) {
    errores.codigo = 'El código no puede superar 20 caracteres.';
  }

  if (!nombre) {
    errores.nombre = 'El nombre es obligatorio.';
  } else if (nombre.length > 150) {
    errores.nombre = 'El nombre no puede superar 150 caracteres.';
  }

  if (!formulario.facultadId) {
    errores.facultadId = 'La facultad es obligatoria.';
  }

  return errores;
}

export default function Carreras() {
  const { tieneRol } = useSesion();
  const esAdmin = tieneRol('ADMINISTRADOR');

  const [facultades, setFacultades] = useState([]);
  const [cargandoFacultades, setCargandoFacultades] = useState(true);
  const [errorFacultades, setErrorFacultades] = useState(null);

  const [facultadFiltro, setFacultadFiltro] = useState('');
  const [carreras, setCarreras] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);

  const [dialogo, setDialogo] = useState(null);
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [erroresCampos, setErroresCampos] = useState({});
  const [errorGuardado, setErrorGuardado] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const [confirmacion, setConfirmacion] = useState(null);
  const [errorDesactivacion, setErrorDesactivacion] = useState(null);
  const [desactivando, setDesactivando] = useState(false);

  const solicitudVigente = useRef(0);

  const cargarFacultades = useCallback(() => {
    setCargandoFacultades(true);
    setErrorFacultades(null);
    listarFacultades()
      .then((datos) => setFacultades(Array.isArray(datos) ? datos : []))
      .catch((fallo) => {
        setFacultades([]);
        setErrorFacultades(mensajeDeError(fallo));
      })
      .finally(() => setCargandoFacultades(false));
  }, []);

  const cargar = useCallback(() => {
    solicitudVigente.current += 1;
    const idSolicitud = solicitudVigente.current;
    setCargando(true);
    setError(null);
    const parametros = facultadFiltro ? { facultadId: Number(facultadFiltro) } : {};
    listarCarreras(parametros)
      .then((datos) => {
        if (idSolicitud !== solicitudVigente.current) return;
        setCarreras(Array.isArray(datos) ? datos : []);
      })
      .catch((fallo) => {
        if (idSolicitud !== solicitudVigente.current) return;
        setError(mensajeDeError(fallo));
      })
      .finally(() => {
        if (idSolicitud !== solicitudVigente.current) return;
        setCargando(false);
      });
  }, [facultadFiltro]);

  useEffect(() => {
    cargarFacultades();
  }, [cargarFacultades]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function cambiarFiltro(valor) {
    setResultado(null);
    setFacultadFiltro(valor);
  }

  function abrirCrear() {
    setResultado(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario(FORMULARIO_VACIO);
    setDialogo({ modo: 'crear' });
  }

  function abrirEditar(carrera) {
    setResultado(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario({
      codigo: carrera.codigo ?? '',
      nombre: carrera.nombre ?? '',
      facultadId: carrera.facultadId == null ? '' : String(carrera.facultadId),
    });
    setDialogo({ modo: 'editar', carrera });
  }

  function cerrarFormulario() {
    if (guardando) return;
    setDialogo(null);
  }

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  async function guardar(evento) {
    evento.preventDefault();
    if (guardando) return;

    const errores = validarFormulario(formulario);
    setErroresCampos(errores);
    if (Object.keys(errores).length > 0) return;

    setGuardando(true);
    setErrorGuardado(null);
    setResultado(null);
    try {
      const datos = {
        codigo: formulario.codigo.trim(),
        nombre: formulario.nombre.trim(),
        facultadId: Number(formulario.facultadId),
      };
      if (dialogo.modo === 'crear') {
        await crearCarrera(datos);
        setResultado({ tipo: 'exito', mensaje: 'Carrera creada correctamente.' });
      } else {
        await actualizarCarrera({
          ...datos,
          id: dialogo.carrera.id,
          activa: dialogo.carrera.activa,
        });
        setResultado({ tipo: 'exito', mensaje: 'Carrera actualizada correctamente.' });
      }
      setDialogo(null);
      setFormulario(FORMULARIO_VACIO);
      cargar();
    } catch (fallo) {
      setErrorGuardado(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  function pedirDesactivacion(carrera) {
    setResultado(null);
    setError(null);
    setErrorDesactivacion(null);
    setConfirmacion({
      carrera,
      titulo: 'Desactivar carrera',
      mensaje: `¿Deseas desactivar la carrera ${carrera.nombre}?`,
      confirmarTexto: 'Desactivar',
    });
  }

  async function desactivar() {
    if (desactivando || !confirmacion) return;
    setDesactivando(true);
    setErrorDesactivacion(null);
    try {
      const carrera = confirmacion.carrera;
      await actualizarCarrera({
        id: carrera.id,
        facultadId: carrera.facultadId,
        codigo: carrera.codigo,
        nombre: carrera.nombre,
        activa: false,
      });
      setConfirmacion(null);
      setResultado({ tipo: 'exito', mensaje: 'Carrera desactivada correctamente.' });
      cargar();
    } catch (fallo) {
      setErrorDesactivacion(mensajeDeError(fallo));
    } finally {
      setDesactivando(false);
    }
  }

  function nombreFacultad(id) {
    const facultad = facultades.find((item) => String(item.id) === String(id));
    if (!facultad) return `Facultad ID ${id}`;
    return `${facultad.nombre} (${facultad.codigo})${facultad.activa ? '' : ' · inactiva'}`;
  }

  const columnas = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Nombre' },
    {
      clave: 'facultad',
      titulo: 'Facultad',
      render: (carrera) => nombreFacultad(carrera.facultadId),
    },
    {
      clave: 'activa',
      titulo: 'Estado',
      render: (carrera) => (
        <span className={`etiqueta ${carrera.activa ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {carrera.activa ? 'Activa' : 'Inactiva'}
        </span>
      ),
    },
  ];

  if (esAdmin) {
    columnas.push({
      clave: 'acciones',
      titulo: 'Acciones',
      render: (carrera) => (
        <div className="acciones">
          <Boton variante="enlace" pequeno onClick={() => abrirEditar(carrera)}>
            Editar
          </Boton>
          {carrera.activa ? (
            <Boton variante="enlace" pequeno onClick={() => pedirDesactivacion(carrera)}>
              Desactivar
            </Boton>
          ) : null}
        </div>
      ),
    });
  }

  const idsFacultades = new Set(facultades.map((facultad) => String(facultad.id)));
  const facultadActualListada = !formulario.facultadId
    || idsFacultades.has(String(formulario.facultadId));
  const tituloDialogo = dialogo?.modo === 'editar' ? 'Editar carrera' : 'Nueva carrera';

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Carreras</h1>
          <p>Consulte y administre las carreras de la institución por facultad.</p>
        </div>
        {esAdmin ? <Boton onClick={abrirCrear}>Nueva carrera</Boton> : null}
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado
        tipo="error"
        mensaje={error}
        onReintentar={dialogo || confirmacion ? null : cargar}
      />

      <section className="tarjeta">
        <h2>Filtro</h2>
        <MensajeEstado tipo="error" mensaje={errorFacultades} onReintentar={cargarFacultades} />
        <div className="rejilla">
          <CampoFormulario id="filtro-facultad" etiqueta="Facultad">
            <select
              id="filtro-facultad"
              value={facultadFiltro}
              disabled={cargandoFacultades}
              onChange={(evento) => cambiarFiltro(evento.target.value)}
            >
              <option value="">Todas las facultades</option>
              {facultades.map((facultad) => (
                <option key={facultad.id} value={facultad.id}>
                  {facultad.nombre} ({facultad.codigo}){facultad.activa ? '' : ' · inactiva'}
                </option>
              ))}
            </select>
          </CampoFormulario>
        </div>
        {cargandoFacultades ? <MensajeEstado mensaje="Cargando facultades…" /> : null}
        {!cargandoFacultades && !errorFacultades && facultades.length === 0 ? (
          <MensajeEstado mensaje="No hay facultades registradas." />
        ) : null}
      </section>

      <section className="tarjeta">
        <h2>Listado</h2>
        <Tabla
          columnas={columnas}
          filas={carreras}
          cargando={cargando}
          error={null}
          vacio={facultadFiltro
            ? 'No se encontraron carreras para la facultad seleccionada.'
            : 'No se encontraron carreras.'}
        />
      </section>

      {dialogo ? (
        <div className="modal__fondo" role="presentation" onClick={cerrarFormulario}>
          <div
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-label={tituloDialogo}
            onClick={(evento) => evento.stopPropagation()}
          >
            <h2>{tituloDialogo}</h2>
            <form onSubmit={guardar} noValidate>
              <div className="rejilla">
                <CampoFormulario
                  id="carrera-codigo"
                  etiqueta="Código"
                  obligatorio
                  value={formulario.codigo}
                  onChange={(evento) => cambiarCampo('codigo', evento.target.value)}
                  error={erroresCampos.codigo}
                  maxLength={20}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="carrera-nombre"
                  etiqueta="Nombre"
                  obligatorio
                  value={formulario.nombre}
                  onChange={(evento) => cambiarCampo('nombre', evento.target.value)}
                  error={erroresCampos.nombre}
                  maxLength={150}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="carrera-facultad"
                  etiqueta="Facultad"
                  obligatorio
                  error={erroresCampos.facultadId}
                >
                  <select
                    id="carrera-facultad"
                    value={formulario.facultadId}
                    required
                    disabled={guardando || cargandoFacultades}
                    onChange={(evento) => cambiarCampo('facultadId', evento.target.value)}
                  >
                    <option value="">Seleccione una facultad</option>
                    {facultadActualListada || !formulario.facultadId ? null : (
                      <option value={formulario.facultadId}>Facultad ID {formulario.facultadId}</option>
                    )}
                    {facultades.map((facultad) => (
                      <option key={facultad.id} value={facultad.id}>
                        {facultad.nombre} ({facultad.codigo}){facultad.activa ? '' : ' · inactiva'}
                      </option>
                    ))}
                  </select>
                </CampoFormulario>
              </div>
              <MensajeEstado tipo="error" mensaje={errorGuardado} />
              <div className="modal__acciones">
                <Boton variante="secundario" onClick={cerrarFormulario} disabled={guardando}>
                  Cancelar
                </Boton>
                <Boton type="submit" cargando={guardando}>
                  {dialogo.modo === 'crear' ? 'Crear' : 'Guardar'}
                </Boton>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {confirmacion ? (
        <Modal
          titulo={confirmacion.titulo}
          mensaje={
            errorDesactivacion
              ? `${confirmacion.mensaje} ${errorDesactivacion}`
              : confirmacion.mensaje
          }
          confirmarTexto={confirmacion.confirmarTexto}
          onConfirmar={desactivar}
          onCancelar={() => {
            if (!desactivando) {
              setConfirmacion(null);
              setErrorDesactivacion(null);
            }
          }}
          cargando={desactivando}
        />
      ) : null}
    </>
  );
}
