import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Tabla from '../../components/comunes/Tabla';
import { mensajeDeError } from '../../services/api';
import {
  actualizarTutor,
  crearTutor,
  listarTutores,
} from '../../services/tutorEmpresarialService';

const TUTOR_VACIO = { usuarioId: '', cargo: '' };

export default function TutoresEmpresa({ empresaId, puedeGestionar = true }) {
  const [tutores, setTutores] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [formulario, setFormulario] = useState(null);
  const [editando, setEditando] = useState(null);
  const [erroresCampos, setErroresCampos] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarTutores(empresaId)
      .then((datos) => setTutores(Array.isArray(datos) ? datos : []))
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
    setResultado(null);
    setError(null);
    setErroresCampos({});
    setEditando(null);
    setFormulario({ ...TUTOR_VACIO });
  }

  function abrirEditar(tutor) {
    setResultado(null);
    setError(null);
    setErroresCampos({});
    setEditando(tutor);
    setFormulario({ cargo: tutor.cargo || '' });
  }

  function cerrarFormulario() {
    setFormulario(null);
    setEditando(null);
  }

  function validar() {
    const errores = {};
    if (!editando) {
      const usuarioId = Number(formulario.usuarioId);
      if (!formulario.usuarioId || !Number.isInteger(usuarioId) || usuarioId <= 0) {
        errores.usuarioId = 'Ingrese un id de usuario válido.';
      }
    }
    return errores;
  }

  async function guardar(evento) {
    evento.preventDefault();
    const errores = validar();
    setErroresCampos(errores);
    if (Object.keys(errores).length > 0) return;

    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      if (editando) {
        await actualizarTutor(empresaId, {
          id: editando.id,
          cargo: formulario.cargo.trim() || null,
        });
      } else {
        await crearTutor(empresaId, {
          usuarioId: Number(formulario.usuarioId),
          cargo: formulario.cargo.trim() || null,
        });
      }
      cerrarFormulario();
      setResultado({
        tipo: 'exito',
        mensaje: editando ? 'Tutor actualizado.' : 'Tutor registrado correctamente.',
      });
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  const columnas = [
    { clave: 'usuarioId', titulo: 'Usuario' },
    {
      clave: 'cargo',
      titulo: 'Cargo',
      render: (tutor) => tutor.cargo || '—',
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (tutor) => (puedeGestionar ? (
        <Boton variante="enlace" pequeno onClick={() => abrirEditar(tutor)}>
          Editar
        </Boton>
      ) : '—'),
    },
  ];

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h2>Tutores empresariales</h2>
          <p>Un usuario solo puede ser tutor de una empresa.</p>
        </div>
        {puedeGestionar && !formulario ? <Boton onClick={abrirCrear}>Nuevo tutor</Boton> : null}
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} onReintentar={formulario ? null : cargar} />

      {formulario ? (
        <form className="formulario__seccion" onSubmit={guardar}>
          <h3>{editando ? 'Editar cargo del tutor' : 'Registrar tutor'}</h3>
          {editando ? (
            <CampoFormulario id="tutor-usuario-actual" etiqueta="Usuario">
              <input id="tutor-usuario-actual" value={editando.usuarioId} disabled />
            </CampoFormulario>
          ) : (
            <CampoFormulario
              id="tutor-usuario"
              etiqueta="ID del usuario"
              obligatorio
              type="number"
              min="1"
              value={formulario.usuarioId}
              onChange={(evento) => cambiarCampo('usuarioId', evento.target.value)}
              error={erroresCampos.usuarioId}
            />
          )}
          <CampoFormulario
            id="tutor-cargo"
            etiqueta="Cargo"
            value={formulario.cargo}
            onChange={(evento) => cambiarCampo('cargo', evento.target.value)}
          />
          <div className="formulario__acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar cambios' : 'Registrar tutor'}
            </Boton>
            <Boton variante="secundario" onClick={cerrarFormulario} disabled={guardando}>
              Cancelar
            </Boton>
          </div>
        </form>
      ) : null}

      <Tabla
        columnas={columnas}
        filas={tutores}
        cargando={cargando}
        error={null}
        vacio="No hay tutores registrados."
      />
    </>
  );
}
