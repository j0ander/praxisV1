import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Modal from '../../components/comunes/Modal';
import Tabla from '../../components/comunes/Tabla';
import { useSesion } from '../../contextos/SesionContexto';
import { mensajeDeError } from '../../services/api';
import {
  actualizarFacultad,
  crearFacultad,
  listarFacultades,
} from '../../services/estructuraService';

const FORMULARIO_VACIO = { codigo: '', nombre: '' };

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

  return errores;
}

export default function Facultades() {
  const { tieneRol } = useSesion();
  const esAdmin = tieneRol('ADMINISTRADOR');

  const [facultades, setFacultades] = useState([]);
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

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarFacultades()
      .then((datos) => setFacultades(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function abrirCrear() {
    setResultado(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario(FORMULARIO_VACIO);
    setDialogo({ modo: 'crear' });
  }

  function abrirEditar(facultad) {
    setResultado(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario({ codigo: facultad.codigo ?? '', nombre: facultad.nombre ?? '' });
    setDialogo({ modo: 'editar', facultad });
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
      if (dialogo.modo === 'crear') {
        await crearFacultad({
          codigo: formulario.codigo.trim(),
          nombre: formulario.nombre.trim(),
        });
        setResultado({ tipo: 'exito', mensaje: 'Facultad creada correctamente.' });
      } else {
        await actualizarFacultad({
          id: dialogo.facultad.id,
          codigo: formulario.codigo.trim(),
          nombre: formulario.nombre.trim(),
          activa: dialogo.facultad.activa,
        });
        setResultado({ tipo: 'exito', mensaje: 'Facultad actualizada correctamente.' });
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

  function pedirDesactivacion(facultad) {
    setResultado(null);
    setError(null);
    setErrorDesactivacion(null);
    setConfirmacion({
      facultad,
      titulo: 'Desactivar facultad',
      mensaje: `¿Desactivar la facultad ${facultad.nombre}? Dejará de ofrecerse para nuevas operaciones.`,
      confirmarTexto: 'Desactivar',
    });
  }

  async function desactivar() {
    if (desactivando || !confirmacion) return;
    setDesactivando(true);
    setErrorDesactivacion(null);
    try {
      const facultad = confirmacion.facultad;
      await actualizarFacultad({
        id: facultad.id,
        codigo: facultad.codigo,
        nombre: facultad.nombre,
        activa: false,
      });
      setConfirmacion(null);
      setResultado({ tipo: 'exito', mensaje: 'Facultad desactivada correctamente.' });
      cargar();
    } catch (fallo) {
      setErrorDesactivacion(mensajeDeError(fallo));
    } finally {
      setDesactivando(false);
    }
  }

  const columnas = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Nombre' },
    {
      clave: 'activa',
      titulo: 'Estado',
      render: (facultad) => (
        <span className={`etiqueta ${facultad.activa ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {facultad.activa ? 'Activa' : 'Inactiva'}
        </span>
      ),
    },
  ];

  if (esAdmin) {
    columnas.push({
      clave: 'acciones',
      titulo: 'Acciones',
      render: (facultad) => (
        <div className="acciones">
          <Boton variante="enlace" pequeno onClick={() => abrirEditar(facultad)}>
            Editar
          </Boton>
          {facultad.activa ? (
            <Boton variante="enlace" pequeno onClick={() => pedirDesactivacion(facultad)}>
              Desactivar
            </Boton>
          ) : null}
        </div>
      ),
    });
  }

  const tituloDialogo = dialogo?.modo === 'editar' ? 'Editar facultad' : 'Nueva facultad';

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Facultades</h1>
          <p>Consulte y administre las facultades de la institución.</p>
        </div>
        {esAdmin ? <Boton onClick={abrirCrear}>Nueva facultad</Boton> : null}
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado
        tipo="error"
        mensaje={error}
        onReintentar={dialogo || confirmacion ? null : cargar}
      />

      <section className="tarjeta">
        <h2>Listado</h2>
        <Tabla
          columnas={columnas}
          filas={facultades}
          cargando={cargando}
          error={null}
          vacio="No se encontraron facultades."
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
                  id="facultad-codigo"
                  etiqueta="Código"
                  obligatorio
                  value={formulario.codigo}
                  onChange={(evento) => cambiarCampo('codigo', evento.target.value)}
                  error={erroresCampos.codigo}
                  maxLength={20}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="facultad-nombre"
                  etiqueta="Nombre"
                  obligatorio
                  value={formulario.nombre}
                  onChange={(evento) => cambiarCampo('nombre', evento.target.value)}
                  error={erroresCampos.nombre}
                  maxLength={150}
                  disabled={guardando}
                />
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
