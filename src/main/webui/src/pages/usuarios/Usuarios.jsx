import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Modal from '../../components/comunes/Modal';
import RutaEnlace from '../../components/comunes/RutaEnlace';
import Tabla from '../../components/comunes/Tabla';
import { nombreRol, ROLES } from '../../constantes';
import { mensajeDeError } from '../../services/api';
import {
  activarUsuario,
  desactivarUsuario,
  listarUsuarios,
} from '../../services/usuarioService';

const FILTROS_INICIALES = { rol: '', activo: '', texto: '' };
const MENSAJE_ADMINISTRADOR = 'Las cuentas de administrador no se pueden desactivar.';

function esAdministrador(usuario) {
  return (usuario.roles || []).includes('ADMINISTRADOR');
}

export default function Usuarios() {
  const [filtros, setFiltros] = useState(FILTROS_INICIALES);
  const [usuarios, setUsuarios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [confirmacion, setConfirmacion] = useState(null);
  const [procesando, setProcesando] = useState(false);
  const [resultado, setResultado] = useState(null);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarUsuarios(filtros)
      .then((datos) => setUsuarios(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, [filtros.rol, filtros.activo, filtros.texto]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function cambiarFiltro(nombre, valor) {
    setFiltros((actuales) => ({ ...actuales, [nombre]: valor }));
  }

  async function confirmarAccion() {
    setProcesando(true);
    setError(null);
    try {
      if (esAdministrador(confirmacion.usuario)) {
        setError(MENSAJE_ADMINISTRADOR);
        setConfirmacion(null);
        return;
      }
      if (confirmacion.accion === 'activar') {
        await activarUsuario(confirmacion.usuario.id);
        setResultado({ tipo: 'exito', mensaje: 'Usuario activado.' });
      } else {
        await desactivarUsuario(confirmacion.usuario.id);
        setResultado({ tipo: 'exito', mensaje: 'Usuario desactivado.' });
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
    {
      clave: 'nombre',
      titulo: 'Usuario',
      render: (usuario) => (
        <>
          <strong>{usuario.nombres} {usuario.apellidos}</strong>
          <br />
          <small>{usuario.correo}</small>
        </>
      ),
    },
    { clave: 'tipoDocumento', titulo: 'Documento' },
    { clave: 'numeroDocumento', titulo: 'N.º documento' },
    {
      clave: 'activo',
      titulo: 'Estado',
      render: (usuario) => (
        <span className={`etiqueta ${usuario.activo ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {usuario.activo ? 'Activo' : 'Inactivo'}
        </span>
      ),
    },
    {
      clave: 'roles',
      titulo: 'Roles',
      render: (usuario) => (usuario.roles || []).map(nombreRol).join(', ') || '—',
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (usuario) => (
        <div className="acciones">
          <RutaEnlace a={`/admin/usuarios/${usuario.id}`} className="boton boton--enlace boton--pequeno">
            Ver / Editar
          </RutaEnlace>
          {esAdministrador(usuario) ? (
            <small>{MENSAJE_ADMINISTRADOR}</small>
          ) : usuario.activo ? (
            <Boton
              variante="enlace"
              pequeno
              onClick={() => setConfirmacion({
                usuario,
                accion: 'desactivar',
                titulo: 'Desactivar usuario',
                mensaje: `¿Desactivar la cuenta de ${usuario.nombres} ${usuario.apellidos}?`,
                confirmarTexto: 'Desactivar',
              })}
            >
              Desactivar
            </Boton>
          ) : (
            <Boton
              variante="enlace"
              pequeno
              onClick={() => setConfirmacion({
                usuario,
                accion: 'activar',
                titulo: 'Activar usuario',
                mensaje: `¿Activar la cuenta de ${usuario.nombres} ${usuario.apellidos}?`,
                confirmarTexto: 'Activar',
              })}
            >
              Activar
            </Boton>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Usuarios</h1>
          <p>Consulte y administre las cuentas del sistema.</p>
        </div>
        <RutaEnlace a="/admin/usuarios/nuevo" className="boton">
          Nuevo usuario
        </RutaEnlace>
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} onReintentar={confirmacion ? null : cargar} />

      <section className="tarjeta">
        <h2>Búsqueda</h2>
        <form
          className="rejilla"
          onSubmit={(evento) => {
            evento.preventDefault();
            cargar();
          }}
        >
          <CampoFormulario
            id="filtro-texto"
            etiqueta="Nombre, apellido, correo o documento"
            value={filtros.texto}
            onChange={(evento) => cambiarFiltro('texto', evento.target.value)}
            placeholder="Buscar…"
          />
          <CampoFormulario id="filtro-rol" etiqueta="Rol">
            <select
              id="filtro-rol"
              value={filtros.rol}
              onChange={(evento) => cambiarFiltro('rol', evento.target.value)}
            >
              <option value="">Todos los roles</option>
              {ROLES.map((rol) => (
                <option key={rol} value={rol}>{nombreRol(rol)}</option>
              ))}
            </select>
          </CampoFormulario>
          <CampoFormulario id="filtro-activo" etiqueta="Estado">
            <select
              id="filtro-activo"
              value={filtros.activo}
              onChange={(evento) => cambiarFiltro('activo', evento.target.value)}
            >
              <option value="">Todos</option>
              <option value="true">Activos</option>
              <option value="false">Inactivos</option>
            </select>
          </CampoFormulario>
          <div className="formulario__acciones">
            <Boton type="submit">Buscar</Boton>
            <Boton
              variante="secundario"
              onClick={() => setFiltros(FILTROS_INICIALES)}
              disabled={Object.values(filtros).every((valor) => !valor)}
            >
              Limpiar
            </Boton>
          </div>
        </form>
      </section>

      <section className="tarjeta">
        <h2>Resultados</h2>
        <Tabla columnas={columnas} filas={usuarios} cargando={cargando} error={null} vacio="No se encontraron usuarios." />
      </section>

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
