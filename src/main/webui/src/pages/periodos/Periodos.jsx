import React, { useCallback, useEffect, useRef, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Modal from '../../components/comunes/Modal';
import Tabla from '../../components/comunes/Tabla';
import { mensajeDeError } from '../../services/api';
import {
  activarPeriodo,
  actualizarPeriodo,
  crearPeriodo,
  desactivarPeriodo,
  listarPeriodos,
} from '../../services/estructuraService';

const FORMULARIO_VACIO = { codigo: '', nombre: '', fechaInicio: '', fechaFin: '' };

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
  } else if (nombre.length > 100) {
    errores.nombre = 'El nombre no puede superar 100 caracteres.';
  }

  if (!formulario.fechaInicio) {
    errores.fechaInicio = 'La fecha de inicio es obligatoria.';
  }

  if (!formulario.fechaFin) {
    errores.fechaFin = 'La fecha de fin es obligatoria.';
  } else if (formulario.fechaInicio && formulario.fechaFin < formulario.fechaInicio) {
    errores.fechaFin = 'La fecha de fin no puede ser anterior a la de inicio.';
  }

  return errores;
}

export default function Periodos() {
  const [periodos, setPeriodos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);

  const [dialogo, setDialogo] = useState(null);
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [erroresCampos, setErroresCampos] = useState({});
  const [errorGuardado, setErrorGuardado] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const [activandoId, setActivandoId] = useState(null);
  const [errorActivacion, setErrorActivacion] = useState(null);

  const [confirmacion, setConfirmacion] = useState(null);
  const [errorDesactivacion, setErrorDesactivacion] = useState(null);
  const [desactivando, setDesactivando] = useState(false);

  const solicitudVigente = useRef(0);

  const cargar = useCallback(() => {
    solicitudVigente.current += 1;
    const idSolicitud = solicitudVigente.current;
    setCargando(true);
    setError(null);
    setErrorActivacion(null);
    listarPeriodos()
      .then((datos) => {
        if (idSolicitud !== solicitudVigente.current) return;
        setPeriodos(Array.isArray(datos) ? datos : []);
      })
      .catch((fallo) => {
        if (idSolicitud !== solicitudVigente.current) return;
        setError(mensajeDeError(fallo));
      })
      .finally(() => {
        if (idSolicitud !== solicitudVigente.current) return;
        setCargando(false);
      });
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function abrirCrear() {
    setResultado(null);
    setErrorActivacion(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario(FORMULARIO_VACIO);
    setDialogo({ modo: 'crear' });
  }

  function abrirEditar(periodo) {
    setResultado(null);
    setErrorActivacion(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario({
      codigo: periodo.codigo ?? '',
      nombre: periodo.nombre ?? '',
      fechaInicio: periodo.fechaInicio ?? '',
      fechaFin: periodo.fechaFin ?? '',
    });
    setDialogo({ modo: 'editar', periodo });
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
    setError(null);
    setErrorActivacion(null);
    setErrorGuardado(null);
    setResultado(null);
    try {
      const datos = {
        codigo: formulario.codigo.trim(),
        nombre: formulario.nombre.trim(),
        fechaInicio: formulario.fechaInicio,
        fechaFin: formulario.fechaFin,
      };
      if (dialogo.modo === 'crear') {
        await crearPeriodo(datos);
        setResultado({ tipo: 'exito', mensaje: 'Período creado correctamente.' });
      } else {
        await actualizarPeriodo({ ...datos, id: dialogo.periodo.id });
        setResultado({ tipo: 'exito', mensaje: 'Período actualizado correctamente.' });
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

  async function activar(periodo) {
    if (activandoId !== null || desactivando || confirmacion || dialogo) return;
    setResultado(null);
    setError(null);
    setErrorActivacion(null);
    setActivandoId(periodo.id);
    try {
      await activarPeriodo(periodo.id);
      setResultado({ tipo: 'exito', mensaje: 'Período activado correctamente.' });
      cargar();
    } catch (fallo) {
      setErrorActivacion(mensajeDeError(fallo));
    } finally {
      setActivandoId(null);
    }
  }

  function pedirDesactivacion(periodo) {
    setResultado(null);
    setError(null);
    setErrorActivacion(null);
    setErrorDesactivacion(null);
    setConfirmacion({
      periodo,
      titulo: 'Desactivar período',
      mensaje: `¿Deseas desactivar el período ${periodo.nombre}?`,
      confirmarTexto: 'Desactivar',
    });
  }

  async function desactivar() {
    if (desactivando || !confirmacion) return;
    setDesactivando(true);
    setErrorActivacion(null);
    setErrorDesactivacion(null);
    try {
      await desactivarPeriodo(confirmacion.periodo.id);
      setConfirmacion(null);
      setResultado({ tipo: 'exito', mensaje: 'Período desactivado correctamente.' });
      cargar();
    } catch (fallo) {
      setErrorDesactivacion(mensajeDeError(fallo));
    } finally {
      setDesactivando(false);
    }
  }

  const ocupado = guardando || activandoId !== null || desactivando || confirmacion !== null;
  const tituloDialogo = dialogo?.modo === 'editar' ? 'Editar período' : 'Nuevo período';

  const columnas = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Nombre' },
    {
      clave: 'vigencia',
      titulo: 'Vigencia',
      render: (periodo) => `${periodo.fechaInicio || '—'} a ${periodo.fechaFin || '—'}`,
    },
    {
      clave: 'activo',
      titulo: 'Estado',
      render: (periodo) => (
        <span className={`etiqueta ${periodo.activo ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {periodo.activo ? 'Activo' : 'Inactivo'}
        </span>
      ),
    },
  ];

  columnas.push({
    clave: 'acciones',
    titulo: 'Acciones',
    render: (periodo) => (
      <div className="acciones">
        <Boton
          variante="enlace"
          pequeno
          disabled={ocupado}
          onClick={() => abrirEditar(periodo)}
        >
          Editar
        </Boton>
        {periodo.activo ? (
          <Boton
            variante="enlace"
            pequeno
            disabled={ocupado}
            onClick={() => pedirDesactivacion(periodo)}
          >
            Desactivar
          </Boton>
        ) : (
          <Boton
            variante="enlace"
            pequeno
            cargando={activandoId === periodo.id}
            disabled={ocupado && activandoId !== periodo.id}
            onClick={() => activar(periodo)}
          >
            Activar
          </Boton>
        )}
      </div>
    ),
  });

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Períodos académicos</h1>
          <p>Registre los períodos académicos y fije el vigente.</p>
        </div>
        <Boton onClick={abrirCrear} disabled={ocupado}>
          Nuevo período
        </Boton>
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado
        tipo="error"
        mensaje={error}
        onReintentar={dialogo || confirmacion ? null : cargar}
      />
      <MensajeEstado tipo="error" mensaje={errorActivacion} />

      <section className="tarjeta">
        <h2>Listado</h2>
        <Tabla
          columnas={columnas}
          filas={periodos}
          cargando={cargando}
          error={null}
          vacio="No se encontraron períodos."
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
                  id="periodo-codigo"
                  etiqueta="Código"
                  obligatorio
                  value={formulario.codigo}
                  onChange={(evento) => cambiarCampo('codigo', evento.target.value)}
                  error={erroresCampos.codigo}
                  maxLength={20}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="periodo-nombre"
                  etiqueta="Nombre"
                  obligatorio
                  value={formulario.nombre}
                  onChange={(evento) => cambiarCampo('nombre', evento.target.value)}
                  error={erroresCampos.nombre}
                  maxLength={100}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="periodo-fecha-inicio"
                  etiqueta="Fecha de inicio"
                  type="date"
                  obligatorio
                  value={formulario.fechaInicio}
                  onChange={(evento) => cambiarCampo('fechaInicio', evento.target.value)}
                  error={erroresCampos.fechaInicio}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="periodo-fecha-fin"
                  etiqueta="Fecha de fin"
                  type="date"
                  obligatorio
                  value={formulario.fechaFin}
                  onChange={(evento) => cambiarCampo('fechaFin', evento.target.value)}
                  error={erroresCampos.fechaFin}
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
