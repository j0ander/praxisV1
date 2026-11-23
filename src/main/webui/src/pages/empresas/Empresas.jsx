import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import RutaEnlace from '../../components/comunes/RutaEnlace';
import Tabla from '../../components/comunes/Tabla';
import { mensajeDeError } from '../../services/api';
import { crearEmpresa, listarEmpresas } from '../../services/empresaService';
import { validarDatosEmpresa } from './validacionesEmpresa';

const FORMULARIO_VACIO = {
  ruc: '',
  razonSocial: '',
  nombreComercial: '',
  direccion: '',
  telefono: '',
  correo: '',
};

export default function Empresas() {
  const [texto, setTexto] = useState('');
  const [filtro, setFiltro] = useState('');
  const [empresas, setEmpresas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [mostrandoFormulario, setMostrandoFormulario] = useState(false);
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [erroresCampos, setErroresCampos] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarEmpresas(filtro)
      .then((datos) => setEmpresas(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, [filtro]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  function buscar(evento) {
    evento.preventDefault();
    setResultado(null);
    setFiltro(texto.trim());
  }

  function limpiar() {
    setTexto('');
    setFiltro('');
  }

  function abrirFormulario() {
    setResultado(null);
    setError(null);
    setErroresCampos({});
    setFormulario(FORMULARIO_VACIO);
    setMostrandoFormulario(true);
  }

  async function guardar(evento) {
    evento.preventDefault();
    const errores = validarDatosEmpresa(formulario);
    setErroresCampos(errores);
    if (Object.keys(errores).length > 0) return;

    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      await crearEmpresa({
        ruc: formulario.ruc.trim(),
        razonSocial: formulario.razonSocial.trim(),
        nombreComercial: formulario.nombreComercial.trim() || null,
        direccion: formulario.direccion.trim() || null,
        telefono: formulario.telefono.trim() || null,
        correo: formulario.correo.trim() || null,
      });
      setMostrandoFormulario(false);
      setFormulario(FORMULARIO_VACIO);
      setResultado({ tipo: 'exito', mensaje: 'Empresa creada correctamente.' });
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  const columnas = [
    { clave: 'ruc', titulo: 'RUC' },
    { clave: 'razonSocial', titulo: 'Razón social' },
    {
      clave: 'activa',
      titulo: 'Estado',
      render: (empresa) => (
        <span className={`etiqueta ${empresa.activa ? 'etiqueta--activo' : 'etiqueta--inactivo'}`}>
          {empresa.activa ? 'Activa' : 'Inactiva'}
        </span>
      ),
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (empresa) => (
        <RutaEnlace a={`/empresas/${empresa.id}`} className="boton boton--enlace boton--pequeno">
          Ver detalle
        </RutaEnlace>
      ),
    },
  ];

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Empresas</h1>
          <p>Consulte y registre las empresas receptoras de prácticas.</p>
        </div>
        {!mostrandoFormulario ? (
          <Boton onClick={abrirFormulario}>Nueva empresa</Boton>
        ) : null}
      </div>

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      <MensajeEstado tipo="error" mensaje={error} onReintentar={mostrandoFormulario ? null : cargar} />

      {mostrandoFormulario ? (
        <form className="tarjeta" onSubmit={guardar}>
          <h2>Nueva empresa</h2>
          <div className="rejilla">
            <CampoFormulario
              id="empresa-ruc"
              etiqueta="RUC"
              obligatorio
              value={formulario.ruc}
              onChange={(evento) => cambiarCampo('ruc', evento.target.value)}
              error={erroresCampos.ruc}
              maxLength={13}
            />
            <CampoFormulario
              id="empresa-razon-social"
              etiqueta="Razón social"
              obligatorio
              value={formulario.razonSocial}
              onChange={(evento) => cambiarCampo('razonSocial', evento.target.value)}
              error={erroresCampos.razonSocial}
            />
            <CampoFormulario
              id="empresa-nombre-comercial"
              etiqueta="Nombre comercial"
              value={formulario.nombreComercial}
              onChange={(evento) => cambiarCampo('nombreComercial', evento.target.value)}
            />
            <CampoFormulario
              id="empresa-direccion"
              etiqueta="Dirección"
              value={formulario.direccion}
              onChange={(evento) => cambiarCampo('direccion', evento.target.value)}
            />
            <CampoFormulario
              id="empresa-telefono"
              etiqueta="Teléfono"
              value={formulario.telefono}
              onChange={(evento) => cambiarCampo('telefono', evento.target.value)}
            />
            <CampoFormulario
              id="empresa-correo"
              etiqueta="Correo"
              type="email"
              value={formulario.correo}
              onChange={(evento) => cambiarCampo('correo', evento.target.value)}
            />
          </div>
          <div className="formulario__acciones">
            <Boton type="submit" cargando={guardando}>Crear empresa</Boton>
            <Boton
              variante="secundario"
              onClick={() => setMostrandoFormulario(false)}
              disabled={guardando}
            >
              Cancelar
            </Boton>
          </div>
        </form>
      ) : null}

      <section className="tarjeta">
        <h2>Búsqueda</h2>
        <form className="rejilla" onSubmit={buscar}>
          <CampoFormulario
            id="filtro-empresa"
            etiqueta="RUC o razón social"
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
            placeholder="Buscar…"
          />
          <div className="formulario__acciones">
            <Boton type="submit">Buscar</Boton>
            <Boton variante="secundario" onClick={limpiar} disabled={!texto && !filtro}>
              Limpiar
            </Boton>
          </div>
        </form>
      </section>

      <section className="tarjeta">
        <h2>Resultados</h2>
        <Tabla
          columnas={columnas}
          filas={empresas}
          cargando={cargando}
          error={null}
          vacio="No se encontraron empresas."
        />
      </section>
    </>
  );
}
