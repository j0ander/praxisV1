import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Tabla from '../../components/comunes/Tabla';
import { nombreRolEmpresa, ROLES_EMPRESA } from '../../constantes';
import { mensajeDeError } from '../../services/api';
import {
  actualizarMiembro,
  listarMiembros,
  vincularMiembro,
} from '../../services/miembroService';

const MIEMBRO_VACIO = { usuarioId: '', rolEmpresa: 'PUBLICADOR', activo: true };

export default function MiembrosEmpresa({ empresaId, puedeGestionar = true }) {
  const [miembros, setMiembros] = useState([]);
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
    listarMiembros(empresaId)
      .then((datos) => setMiembros(Array.isArray(datos) ? datos : []))
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
    setFormulario({ ...MIEMBRO_VACIO });
  }

  function abrirEditar(miembro) {
    setResultado(null);
    setError(null);
    setErroresCampos({});
    setEditando(miembro);
    setFormulario({
      rolEmpresa: miembro.rolEmpresa,
      activo: Boolean(miembro.activo),
    });
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
    if (!ROLES_EMPRESA.includes(formulario.rolEmpresa)) {
      errores.rolEmpresa = 'Seleccione un rol de empresa válido.';
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
        await actualizarMiembro(empresaId, editando.id, {
          rolEmpresa: formulario.rolEmpresa,
          activo: Boolean(formulario.activo),
        });
      } else {
        await vincularMiembro(empresaId, {
          usuarioId: Number(formulario.usuarioId),
          rolEmpresa: formulario.rolEmpresa,
          activo: true,
        });
      }
      cerrarFormulario();
      setResultado({
        tipo: 'exito',
        mensaje: editando ? 'Miembro actualizado.' : 'Miembro vinculado correctamente.',
      });
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  const administradoresActivos = miembros.filter(
    (miembro) => miembro.activo && miembro.rolEmpresa === 'ADMIN_EMPRESA',
  );
  const editandoUltimoAdmin = Boolean(editando)
    && editando.activo
    && editando.rolEmpresa === 'ADMIN_EMPRESA'
    && administradoresActivos.length === 1;
  const cambioRiesgoso = editandoUltimoAdmin
    && (!formulario?.activo || formulario?.rolEmpresa !== 'ADMIN_EMPRESA');

  const columnas = [
    {
      clave: 'usuario',
      titulo: 'Usuario',
      render: (miembro) => (
        <>
          <strong>{miembro.nombres} {miembro.apellidos}</strong>
          <br />
          <small>{miembro.correo}</small>
        </>
      ),
    },
    {
      clave: 'rolEmpresa',
      titulo: 'Rol de empresa',
      render: (miembro) => nombreRolEmpresa(miembro.rolEmpresa),
    },
    {
      clave: 'activo',
      titulo: 'Estado',
      render: (miembro) => (
        <span className={`etiqueta ${miembro.activo ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {miembro.activo ? 'Activo' : 'Inactivo'}
        </span>
      ),
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (miembro) => (puedeGestionar ? (
        <Boton variante="enlace" pequeno onClick={() => abrirEditar(miembro)}>
          Editar
        </Boton>
      ) : '—'),
    },
  ];

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h2>Miembros</h2>
          <p>Las bajas son lógicas; la empresa debe conservar un ADMIN_EMPRESA activo.</p>
        </div>
        {puedeGestionar && !formulario ? <Boton onClick={abrirCrear}>Vincular miembro</Boton> : null}
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} onReintentar={formulario ? null : cargar} />

      {formulario ? (
        <form className="formulario__seccion" onSubmit={guardar}>
          <h3>{editando ? 'Actualizar miembro' : 'Vincular miembro'}</h3>
          {editando ? (
            <CampoFormulario id="miembro-usuario-actual" etiqueta="Usuario">
              <input id="miembro-usuario-actual" value={editando.usuarioId} disabled />
            </CampoFormulario>
          ) : (
            <CampoFormulario
              id="miembro-usuario"
              etiqueta="ID del usuario"
              obligatorio
              type="number"
              min="1"
              value={formulario.usuarioId}
              onChange={(evento) => cambiarCampo('usuarioId', evento.target.value)}
              error={erroresCampos.usuarioId}
            />
          )}
          <CampoFormulario id="miembro-rol" etiqueta="Rol de empresa" obligatorio error={erroresCampos.rolEmpresa}>
            <select
              id="miembro-rol"
              value={formulario.rolEmpresa}
              onChange={(evento) => cambiarCampo('rolEmpresa', evento.target.value)}
            >
              {ROLES_EMPRESA.map((rol) => (
                <option key={rol} value={rol}>{nombreRolEmpresa(rol)}</option>
              ))}
            </select>
          </CampoFormulario>
          {editando ? (
            <div className="opcion">
              <input
                id="miembro-activo"
                type="checkbox"
                checked={formulario.activo}
                onChange={(evento) => cambiarCampo('activo', evento.target.checked)}
              />
              <label htmlFor="miembro-activo">Miembro activo</label>
            </div>
          ) : (
            <p>El miembro se vincula activo.</p>
          )}
          {cambioRiesgoso ? (
            <MensajeEstado
              tipo="info"
              mensaje="Es el último ADMIN_EMPRESA activo: desactivarlo o degradarlo será rechazado por el backend."
            />
          ) : null}
          <div className="formulario__acciones">
            <Boton type="submit" cargando={guardando}>
              {editando ? 'Guardar cambios' : 'Vincular'}
            </Boton>
            <Boton variante="secundario" onClick={cerrarFormulario} disabled={guardando}>
              Cancelar
            </Boton>
          </div>
        </form>
      ) : null}

      <Tabla
        columnas={columnas}
        filas={miembros}
        cargando={cargando}
        error={null}
        vacio="No hay miembros registrados."
      />
    </>
  );
}
