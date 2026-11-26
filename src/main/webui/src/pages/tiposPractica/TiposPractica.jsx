import React, { useCallback, useEffect, useRef, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Modal from '../../components/comunes/Modal';
import Tabla from '../../components/comunes/Tabla';
import { mensajeDeError } from '../../services/api';
import {
  actualizarTipoPractica,
  crearTipoPractica,
  listarTiposPractica,
} from '../../services/estructuraService';

const FORMULARIO_VACIO = { nombre: '', horasRequeridas: '', notaMinima: '' };
const DECIMAL_MAXIMO_DOS_CIFRAS = /^[+-]?(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/;

function validarFormulario(formulario) {
  const errores = {};
  const nombre = formulario.nombre.trim();
  const horas = formulario.horasRequeridas.trim();
  const nota = formulario.notaMinima.trim();

  if (!nombre) {
    errores.nombre = 'El nombre es obligatorio.';
  } else if (nombre.length > 80) {
    errores.nombre = 'El nombre no puede superar 80 caracteres.';
  }

  if (!horas) {
    errores.horasRequeridas = 'Las horas requeridas son obligatorias.';
  } else if (!DECIMAL_MAXIMO_DOS_CIFRAS.test(horas)) {
    errores.horasRequeridas = 'Se permiten como máximo dos decimales.';
  } else {
    const valorHoras = Number(horas);
    if (!Number.isFinite(valorHoras) || valorHoras <= 0) {
      errores.horasRequeridas = 'Las horas requeridas deben ser mayores que cero.';
    }
  }

  if (!nota) {
    errores.notaMinima = 'La nota mínima es obligatoria.';
  } else if (!DECIMAL_MAXIMO_DOS_CIFRAS.test(nota)) {
    errores.notaMinima = 'Se permiten como máximo dos decimales.';
  } else {
    const valorNota = Number(nota);
    if (!Number.isFinite(valorNota) || valorNota < 0 || valorNota > 20) {
      errores.notaMinima = 'La nota mínima debe estar entre 0 y 20.';
    }
  }

  return errores;
}

export default function TiposPractica() {
  const [tipos, setTipos] = useState([]);
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

  const cargar = useCallback(() => {
    solicitudVigente.current += 1;
    const idSolicitud = solicitudVigente.current;
    setCargando(true);
    setError(null);
    listarTiposPractica()
      .then((datos) => {
        if (idSolicitud !== solicitudVigente.current) return;
        setTipos(Array.isArray(datos) ? datos : []);
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
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario(FORMULARIO_VACIO);
    setDialogo({ modo: 'crear' });
  }

  function abrirEditar(tipo) {
    setResultado(null);
    setErrorGuardado(null);
    setErroresCampos({});
    setFormulario({
      nombre: tipo.nombre ?? '',
      horasRequeridas: tipo.horasRequeridas == null ? '' : String(tipo.horasRequeridas),
      notaMinima: tipo.notaMinima == null ? '' : String(tipo.notaMinima),
    });
    setDialogo({ modo: 'editar', tipo });
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
    setErrorGuardado(null);
    setResultado(null);
    try {
      const datos = {
        nombre: formulario.nombre.trim(),
        horasRequeridas: formulario.horasRequeridas.trim(),
        notaMinima: formulario.notaMinima.trim(),
      };
      if (dialogo.modo === 'crear') {
        await crearTipoPractica(datos);
        setResultado({ tipo: 'exito', mensaje: 'Tipo de práctica creado correctamente.' });
      } else {
        await actualizarTipoPractica({
          ...datos,
          id: dialogo.tipo.id,
          activo: dialogo.tipo.activo,
        });
        setResultado({ tipo: 'exito', mensaje: 'Tipo de práctica actualizado correctamente.' });
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

  function pedirDesactivacion(tipo) {
    setResultado(null);
    setError(null);
    setErrorGuardado(null);
    setErrorDesactivacion(null);
    setConfirmacion({
      tipo,
      titulo: 'Desactivar tipo de práctica',
      mensaje: `¿Deseas desactivar el tipo de práctica ${tipo.nombre}?`,
      confirmarTexto: 'Desactivar',
    });
  }

  async function desactivar() {
    if (desactivando || !confirmacion) return;
    setDesactivando(true);
    setErrorDesactivacion(null);
    try {
      const tipo = confirmacion.tipo;
      await actualizarTipoPractica({
        id: tipo.id,
        nombre: tipo.nombre,
        horasRequeridas: tipo.horasRequeridas,
        notaMinima: tipo.notaMinima,
        activo: false,
      });
      setConfirmacion(null);
      setResultado({ tipo: 'exito', mensaje: 'Tipo de práctica desactivado correctamente.' });
      cargar();
    } catch (fallo) {
      setErrorDesactivacion(mensajeDeError(fallo));
    } finally {
      setDesactivando(false);
    }
  }

  const ocupado = guardando || desactivando || confirmacion !== null;
  const tituloDialogo = dialogo?.modo === 'editar' ? 'Editar tipo de práctica' : 'Nuevo tipo de práctica';

  const columnas = [
    { clave: 'nombre', titulo: 'Nombre' },
    { clave: 'horasRequeridas', titulo: 'Horas requeridas' },
    { clave: 'notaMinima', titulo: 'Nota mínima' },
    {
      clave: 'activo',
      titulo: 'Estado',
      render: (tipo) => (
        <span className={`etiqueta ${tipo.activo ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {tipo.activo ? 'Activo' : 'Inactivo'}
        </span>
      ),
    },
  ];

  columnas.push({
    clave: 'acciones',
    titulo: 'Acciones',
    render: (tipo) => (
      <div className="acciones">
        <Boton
          variante="enlace"
          pequeno
          disabled={ocupado}
          onClick={() => abrirEditar(tipo)}
        >
          Editar
        </Boton>
        {tipo.activo ? (
          <Boton
            variante="enlace"
            pequeno
            disabled={ocupado}
            onClick={() => pedirDesactivacion(tipo)}
          >
            Desactivar
          </Boton>
        ) : null}
      </div>
    ),
  });

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Tipos de práctica</h1>
          <p>Mantenga el catálogo con sus horas requeridas y nota mínima.</p>
        </div>
        <Boton onClick={abrirCrear} disabled={ocupado}>
          Nuevo tipo de práctica
        </Boton>
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
          filas={tipos}
          cargando={cargando}
          error={null}
          vacio="No se encontraron tipos de práctica."
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
                  id="tipo-practica-nombre"
                  etiqueta="Nombre"
                  obligatorio
                  value={formulario.nombre}
                  onChange={(evento) => cambiarCampo('nombre', evento.target.value)}
                  error={erroresCampos.nombre}
                  maxLength={80}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="tipo-practica-horas"
                  etiqueta="Horas requeridas"
                  type="number"
                  step="0.01"
                  min="0.01"
                  obligatorio
                  value={formulario.horasRequeridas}
                  onChange={(evento) => cambiarCampo('horasRequeridas', evento.target.value)}
                  error={erroresCampos.horasRequeridas}
                  disabled={guardando}
                />
                <CampoFormulario
                  id="tipo-practica-nota"
                  etiqueta="Nota mínima"
                  type="number"
                  step="0.01"
                  min="0"
                  max="20"
                  obligatorio
                  value={formulario.notaMinima}
                  onChange={(evento) => cambiarCampo('notaMinima', evento.target.value)}
                  error={erroresCampos.notaMinima}
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
