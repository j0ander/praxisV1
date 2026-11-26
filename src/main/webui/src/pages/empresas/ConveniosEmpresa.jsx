import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Modal from '../../components/comunes/Modal';
import Tabla from '../../components/comunes/Tabla';
import { mensajeDeError } from '../../services/api';
import {
  actualizarConvenio,
  crearConvenio,
  listarConveniosPorEmpresa,
  suspenderConvenio,
  terminarConvenio,
} from '../../services/convenioService';

const CONVENIO_VACIO = { numero: '', fechaInicio: '', fechaFin: '', documentoUrl: '' };

function desdeConvenio(convenio) {
  return {
    numero: convenio.numero || '',
    fechaInicio: convenio.fechaInicio || '',
    fechaFin: convenio.fechaFin || '',
    documentoUrl: convenio.documentoUrl || '',
  };
}

function estadoEfectivo(convenio) {
  return convenio.estadoEfectivo || convenio.estado;
}

function esEditable(convenio) {
  return estadoEfectivo(convenio) === 'VIGENTE';
}

function esTerminable(convenio) {
  return ['VIGENTE', 'SUSPENDIDO', 'VENCIDO'].includes(estadoEfectivo(convenio));
}

export default function ConveniosEmpresa({ empresaId, puedeEditar = true }) {
  const [convenios, setConvenios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [formulario, setFormulario] = useState(null);
  const [editando, setEditando] = useState(null);
  const [erroresCampos, setErroresCampos] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [confirmacion, setConfirmacion] = useState(null);
  const [procesando, setProcesando] = useState(false);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarConveniosPorEmpresa(empresaId)
      .then((datos) => setConvenios(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, [empresaId]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  function abrirCrear() {
    if (!puedeEditar) return;
    setResultado(null);
    setError(null);
    setErroresCampos({});
    setEditando(null);
    setFormulario({ ...CONVENIO_VACIO });
  }

  function abrirEditar(convenio) {
    if (!puedeEditar) return;
    setResultado(null);
    setError(null);
    setErroresCampos({});
    setEditando(convenio);
    setFormulario(desdeConvenio(convenio));
  }

  function cerrarFormulario() {
    setFormulario(null);
    setEditando(null);
  }

  function validar() {
    const errores = {};
    if (!formulario.numero.trim()) {
      errores.numero = 'El número de convenio es obligatorio.';
    }
    if (!formulario.fechaInicio) {
      errores.fechaInicio = 'La fecha de inicio es obligatoria.';
    }
    if (!formulario.fechaFin) {
      errores.fechaFin = 'La fecha de fin es obligatoria.';
    }
    if (formulario.fechaInicio && formulario.fechaFin && formulario.fechaFin < formulario.fechaInicio) {
      errores.fechaFin = 'La fecha de fin no puede ser anterior a la de inicio.';
    }
    return errores;
  }

  async function guardar(evento) {
    evento.preventDefault();
    if (!puedeEditar) return;
    const errores = validar();
    setErroresCampos(errores);
    if (Object.keys(errores).length > 0) return;

    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      const cuerpo = {
        empresaId: Number(empresaId),
        numero: formulario.numero.trim(),
        fechaInicio: formulario.fechaInicio,
        fechaFin: formulario.fechaFin,
        documentoUrl: formulario.documentoUrl.trim() || null,
      };
      if (editando) {
        cuerpo.id = editando.id;
        await actualizarConvenio(cuerpo);
      } else {
        await crearConvenio(cuerpo);
      }
      cerrarFormulario();
      setResultado({
        tipo: 'exito',
        mensaje: editando ? 'Convenio actualizado.' : 'Convenio creado correctamente.',
      });
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarAccion() {
    if (!puedeEditar) return;
    setProcesando(true);
    setError(null);
    setResultado(null);
    try {
      if (confirmacion.accion === 'suspender') {
        await suspenderConvenio(confirmacion.convenio.id);
        setResultado({ tipo: 'exito', mensaje: 'Convenio suspendido.' });
      } else {
        await terminarConvenio(confirmacion.convenio.id);
        setResultado({ tipo: 'exito', mensaje: 'Convenio terminado.' });
      }
      setConfirmacion(null);
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setProcesando(false);
    }
  }

  const columnas = [
    { clave: 'numero', titulo: 'Número' },
    {
      clave: 'vigencia',
      titulo: 'Vigencia',
      render: (convenio) => `${convenio.fechaInicio || '—'} a ${convenio.fechaFin || '—'}`,
    },
    {
      clave: 'estadoEfectivo',
      titulo: 'Estado efectivo',
      render: (convenio) => {
        const efectivo = convenio.estadoEfectivo || convenio.estado || '—';
        const clase = efectivo === 'VIGENTE' ? 'etiqueta--activo' : 'etiqueta--inactivo';
        return <span className={`etiqueta ${clase}`}>{efectivo}</span>;
      },
    },
    {
      clave: 'documentoUrl',
      titulo: 'Documento',
      render: (convenio) => (convenio.documentoUrl ? (
        <a href={convenio.documentoUrl} target="_blank" rel="noreferrer">Abrir</a>
      ) : '—'),
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (convenio) => (
        <div className="acciones">
          {puedeEditar && esEditable(convenio) ? (
            <Boton variante="enlace" pequeno onClick={() => abrirEditar(convenio)}>
              Editar
            </Boton>
          ) : null}
          {puedeEditar && esEditable(convenio) ? (
            <Boton
              variante="enlace"
              pequeno
              onClick={() => setConfirmacion({
                convenio,
                accion: 'suspender',
                titulo: 'Suspender convenio',
                mensaje: `¿Suspender el convenio ${convenio.numero}?`,
                confirmarTexto: 'Suspender',
              })}
            >
              Suspender
            </Boton>
          ) : null}
          {puedeEditar && esTerminable(convenio) ? (
            <Boton
              variante="enlace"
              pequeno
              onClick={() => setConfirmacion({
                convenio,
                accion: 'terminar',
                titulo: 'Terminar convenio',
                mensaje: `¿Terminar el convenio ${convenio.numero}? Esta acción no se puede revertir.`,
                confirmarTexto: 'Terminar',
              })}
            >
              Terminar
            </Boton>
          ) : null}
        </div>
      ),
    },
  ];

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h2>Convenios</h2>
          <p>El estado efectivo incluye los convenios vencidos por fecha.</p>
        </div>
        {puedeEditar && !formulario ? <Boton onClick={abrirCrear}>Nuevo convenio</Boton> : null}
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} onReintentar={formulario || confirmacion ? null : cargar} />

      {formulario ? (
        <form className="formulario__seccion" onSubmit={guardar}>
          <h3>{editando ? 'Editar convenio' : 'Nuevo convenio'}</h3>
          <div className="rejilla">
            <CampoFormulario
              id="convenio-numero"
              etiqueta="Número"
              obligatorio
              value={formulario.numero}
              onChange={(evento) => cambiarCampo('numero', evento.target.value)}
              error={erroresCampos.numero}
            />
            <CampoFormulario
              id="convenio-documento"
              etiqueta="URL del documento"
              value={formulario.documentoUrl}
              onChange={(evento) => cambiarCampo('documentoUrl', evento.target.value)}
            />
            <CampoFormulario
              id="convenio-fecha-inicio"
              etiqueta="Fecha de inicio"
              type="date"
              obligatorio
              value={formulario.fechaInicio}
              onChange={(evento) => cambiarCampo('fechaInicio', evento.target.value)}
              error={erroresCampos.fechaInicio}
            />
            <CampoFormulario
              id="convenio-fecha-fin"
              etiqueta="Fecha de fin"
              type="date"
              obligatorio
              value={formulario.fechaFin}
              onChange={(evento) => cambiarCampo('fechaFin', evento.target.value)}
              error={erroresCampos.fechaFin}
            />
          </div>
          <div className="formulario__acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar cambios' : 'Crear convenio'}
            </Boton>
            <Boton variante="secundario" onClick={cerrarFormulario} disabled={guardando}>
              Cancelar
            </Boton>
          </div>
        </form>
      ) : null}

      <Tabla
        columnas={columnas}
        filas={convenios}
        cargando={cargando}
        error={null}
        vacio="No hay convenios registrados."
      />

      {confirmacion ? (
        <Modal
          titulo={confirmacion.titulo}
          mensaje={confirmacion.mensaje}
          confirmarTexto={confirmacion.confirmarTexto}
          onConfirmar={confirmarAccion}
          onCancelar={() => setConfirmacion(null)}
          cargando={procesando}
        />
      ) : null}
    </>
  );
}
