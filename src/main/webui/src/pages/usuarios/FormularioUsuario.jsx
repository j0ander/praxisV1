import React, { useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import RutaEnlace from '../../components/comunes/RutaEnlace';
import { nombreRol, ROLES, TIPOS_DOCUMENTO } from '../../constantes';
import { mensajeDeError } from '../../services/api';
import { listarCarreras } from '../../services/estructuraService';
import {
  actualizarUsuario,
  asignarCarreras,
  asignarRoles,
  buscarUsuario,
  crearUsuario,
  restablecerPassword,
} from '../../services/usuarioService';
import { navegar } from '../../routes/navegacion';

const FORMULARIO_VACIO = {
  correo: '',
  nombres: '',
  apellidos: '',
  tipoDocumento: 'CEDULA',
  numeroDocumento: '',
  telefono: '',
  alcanceGlobal: false,
  passwordTemporal: '',
};

export default function FormularioUsuario({ id }) {
  const esNuevo = !id;
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [roles, setRoles] = useState([]);
  const [carreraIds, setCarreraIds] = useState([]);
  const [carreras, setCarreras] = useState([]);
  const [cargando, setCargando] = useState(!esNuevo);
  const [guardando, setGuardando] = useState(false);
  const [guardandoPermisos, setGuardandoPermisos] = useState('');
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [modalPassword, setModalPassword] = useState(false);
  const [passwordTemporal, setPasswordTemporal] = useState('');

  useEffect(() => {
    listarCarreras()
      .then((datos) => setCarreras(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)));
  }, []);

  useEffect(() => {
    if (esNuevo) {
      setCargando(false);
      setError(null);
      setFormulario(FORMULARIO_VACIO);
      setRoles([]);
      setCarreraIds([]);
      return;
    }
    let activo = true;
    setCargando(true);
    buscarUsuario(id)
      .then((datos) => {
        if (!activo) return;
        setFormulario({
          correo: datos.correo || '',
          nombres: datos.nombres || '',
          apellidos: datos.apellidos || '',
          tipoDocumento: datos.tipoDocumento || 'CEDULA',
          numeroDocumento: datos.numeroDocumento || '',
          telefono: datos.telefono || '',
          alcanceGlobal: Boolean(datos.alcanceGlobal),
          passwordTemporal: '',
        });
        setRoles(Array.isArray(datos.roles) ? datos.roles : []);
        setCarreraIds((datos.carreraIds || []).map(String));
      })
      .catch((fallo) => {
        if (activo) setError(mensajeDeError(fallo));
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [esNuevo, id]);

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  function alternarRol(rol) {
    setRoles((actuales) => actuales.includes(rol)
      ? actuales.filter((item) => item !== rol)
      : [...actuales, rol]);
  }

  function alternarCarrera(carreraId) {
    setCarreraIds((actuales) => actuales.includes(carreraId)
      ? actuales.filter((item) => item !== carreraId)
      : [...actuales, carreraId]);
  }

  async function guardar(evento) {
    evento.preventDefault();
    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      const cuerpo = { ...formulario };
      if (!esNuevo) {
        cuerpo.id = Number(id);
        delete cuerpo.passwordTemporal;
      }
      const datos = esNuevo ? await crearUsuario(cuerpo) : await actualizarUsuario(cuerpo);
      if (esNuevo && datos?.id) {
        navegar(`/admin/usuarios/${datos.id}`);
        setResultado({ tipo: 'exito', mensaje: 'Usuario creado. Asigne los roles y carreras.' });
      } else {
        setResultado({ tipo: 'exito', mensaje: 'Datos actualizados.' });
      }
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  async function guardarRoles() {
    setGuardandoPermisos('roles');
    setError(null);
    setResultado(null);
    try {
      await asignarRoles(id, roles);
      setResultado({ tipo: 'exito', mensaje: 'Roles actualizados.' });
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardandoPermisos('');
    }
  }

  async function guardarCarreras() {
    setGuardandoPermisos('carreras');
    setError(null);
    setResultado(null);
    try {
      await asignarCarreras(id, carreraIds.map(Number));
      setResultado({ tipo: 'exito', mensaje: 'Carreras actualizadas.' });
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardandoPermisos('');
    }
  }

  async function guardarPassword() {
    setGuardandoPermisos('password');
    setError(null);
    try {
      await restablecerPassword(id, passwordTemporal);
      setModalPassword(false);
      setPasswordTemporal('');
      setResultado({ tipo: 'exito', mensaje: 'Contraseña restablecida.' });
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardandoPermisos('');
    }
  }

  if (cargando) {
    return <MensajeEstado mensaje="Cargando usuario…" />;
  }

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>{esNuevo ? 'Nuevo usuario' : 'Detalle del usuario'}</h1>
          <p>{esNuevo ? 'Registre una cuenta con contraseña temporal.' : 'Administre datos, roles y carreras.'}</p>
        </div>
        <RutaEnlace a="/admin/usuarios" className="boton boton--secundario">
          Volver al listado
        </RutaEnlace>
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} />

      <form className="tarjeta" onSubmit={guardar}>
        <h2>Datos personales</h2>
        <div className="rejilla">
          <CampoFormulario
            id="correo"
            etiqueta="Correo"
            type="email"
            obligatorio
            value={formulario.correo}
            onChange={(evento) => cambiarCampo('correo', evento.target.value)}
          />
          <CampoFormulario
            id="nombres"
            etiqueta="Nombres"
            obligatorio
            value={formulario.nombres}
            onChange={(evento) => cambiarCampo('nombres', evento.target.value)}
          />
          <CampoFormulario
            id="apellidos"
            etiqueta="Apellidos"
            obligatorio
            value={formulario.apellidos}
            onChange={(evento) => cambiarCampo('apellidos', evento.target.value)}
          />
          <CampoFormulario id="tipo-documento" etiqueta="Tipo de documento">
            <select
              id="tipo-documento"
              value={formulario.tipoDocumento}
              onChange={(evento) => cambiarCampo('tipoDocumento', evento.target.value)}
            >
              {TIPOS_DOCUMENTO.map((tipo) => (
                <option key={tipo} value={tipo}>{tipo === 'CEDULA' ? 'Cédula' : 'Pasaporte'}</option>
              ))}
            </select>
          </CampoFormulario>
          <CampoFormulario
            id="numero-documento"
            etiqueta="Número de documento"
            value={formulario.numeroDocumento}
            onChange={(evento) => cambiarCampo('numeroDocumento', evento.target.value)}
          />
          <CampoFormulario
            id="telefono"
            etiqueta="Teléfono"
            value={formulario.telefono}
            onChange={(evento) => cambiarCampo('telefono', evento.target.value)}
          />
        </div>
        <div className="opcion">
          <input
            id="alcance-global"
            type="checkbox"
            checked={formulario.alcanceGlobal}
            onChange={(evento) => cambiarCampo('alcanceGlobal', evento.target.checked)}
          />
          <label htmlFor="alcance-global">Alcance global</label>
        </div>
        {esNuevo ? (
          <CampoFormulario
            id="password-temporal"
            etiqueta="Contraseña temporal"
            type="password"
            obligatorio
            value={formulario.passwordTemporal}
            onChange={(evento) => cambiarCampo('passwordTemporal', evento.target.value)}
          />
        ) : null}
        <div className="formulario__acciones">
          <Boton type="submit" cargando={guardando}>
            {esNuevo ? 'Crear usuario' : 'Guardar cambios'}
          </Boton>
        </div>
      </form>

      {!esNuevo ? (
        <>
          <section className="tarjeta">
            <h2>Roles</h2>
            <p>La asignación del rol DOCENTE crea automáticamente su perfil.</p>
            <div className="lista-opciones">
              {ROLES.map((rol) => (
                <label key={rol} className="opcion">
                  <input
                    type="checkbox"
                    checked={roles.includes(rol)}
                    onChange={() => alternarRol(rol)}
                  />
                  {nombreRol(rol)}
                </label>
              ))}
            </div>
            <div className="formulario__acciones">
              <Boton onClick={guardarRoles} cargando={guardandoPermisos === 'roles'}>
                Guardar roles
              </Boton>
            </div>
          </section>

          <section className="tarjeta">
            <h2>Carreras del ámbito</h2>
            {carreras.length === 0 ? (
              <MensajeEstado mensaje="No hay carreras registradas." />
            ) : (
              <div className="lista-opciones">
                {carreras.map((carrera) => (
                  <label key={carrera.id} className="opcion">
                    <input
                      type="checkbox"
                      checked={carreraIds.includes(String(carrera.id))}
                      onChange={() => alternarCarrera(String(carrera.id))}
                    />
                    {carrera.nombre} ({carrera.codigo}){carrera.activa ? '' : ' · inactiva'}
                  </label>
                ))}
              </div>
            )}
            <div className="formulario__acciones">
              <Boton onClick={guardarCarreras} cargando={guardandoPermisos === 'carreras'}>
                Guardar carreras
              </Boton>
            </div>
          </section>

          <section className="tarjeta">
            <h2>Contraseña</h2>
            <p>El usuario deberá cambiar la contraseña en su próximo ingreso.</p>
            <Boton variante="secundario" onClick={() => setModalPassword(true)}>
              Restablecer contraseña
            </Boton>
          </section>
        </>
      ) : null}

      {modalPassword ? (
        <div className="modal__fondo" role="presentation" onClick={() => setModalPassword(false)}>
          <div className="modal" role="dialog" aria-modal="true" aria-label="Restablecer contraseña" onClick={(evento) => evento.stopPropagation()}>
            <h2>Restablecer contraseña</h2>
            <CampoFormulario
              id="nueva-password-temporal"
              etiqueta="Contraseña temporal"
              type="password"
              obligatorio
              value={passwordTemporal}
              onChange={(evento) => setPasswordTemporal(evento.target.value)}
            />
            <div className="modal__acciones">
              <Boton variante="secundario" onClick={() => setModalPassword(false)} disabled={guardandoPermisos === 'password'}>
                Cancelar
              </Boton>
              <Boton onClick={guardarPassword} cargando={guardandoPermisos === 'password'} disabled={!passwordTemporal}>
                Restablecer
              </Boton>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
