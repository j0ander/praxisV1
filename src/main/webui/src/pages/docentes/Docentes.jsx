import React, { useCallback, useEffect, useState } from 'react';

import Boton from '../../components/comunes/Boton';
import CampoFormulario from '../../components/comunes/CampoFormulario';
import MensajeEstado from '../../components/comunes/MensajeEstado';
import Tabla from '../../components/comunes/Tabla';
import { actualizarDocente, listarDocentes } from '../../services/perfilService';
import { mensajeDeError } from '../../services/api';

const FORMULARIO_VACIO = { codigoDocente: '', dedicacion: '' };

export default function Docentes() {
  const [docentes, setDocentes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [docente, setDocente] = useState(null);
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    listarDocentes()
      .then((datos) => setDocentes(Array.isArray(datos) ? datos : []))
      .catch((fallo) => setError(mensajeDeError(fallo)))
      .finally(() => setCargando(false));
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function abrirEdicion(item) {
    setResultado(null);
    setDocente(item);
    setFormulario({
      codigoDocente: item.codigoDocente || '',
      dedicacion: item.dedicacion || '',
    });
  }

  function cambiarCampo(nombre, valor) {
    setFormulario((actual) => ({ ...actual, [nombre]: valor }));
  }

  async function guardar(evento) {
    evento.preventDefault();
    setGuardando(true);
    setError(null);
    setResultado(null);
    try {
      await actualizarDocente({
        id: docente.id,
        usuarioId: docente.usuarioId,
        codigoDocente: formulario.codigoDocente.trim() || null,
        dedicacion: formulario.dedicacion.trim() || null,
      });
      setDocente(null);
      setResultado({ tipo: 'exito', mensaje: 'Perfil actualizado.' });
      cargar();
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  const columnas = [
    {
      clave: 'codigoDocente',
      titulo: 'Código',
      render: (item) => item.codigoDocente || '—',
    },
    {
      clave: 'dedicacion',
      titulo: 'Dedicación',
      render: (item) => item.dedicacion || '—',
    },
    { clave: 'usuarioId', titulo: 'Usuario', render: (item) => item.usuarioId },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      render: (item) => (
        <Boton variante="enlace" pequeno onClick={() => abrirEdicion(item)}>
          Ver / Editar
        </Boton>
      ),
    },
  ];

  return (
    <>
      <div className="tarjeta__cabecera">
        <div>
          <h1>Docentes</h1>
          <p>Consulte y actualice los perfiles académicos de los docentes.</p>
        </div>
      </div>
      <MensajeEstado tipo="info" mensaje="El perfil de docente se crea automáticamente al asignar el rol DOCENTE." />

      <MensajeEstado tipo={resultado?.tipo} mensaje={resultado?.mensaje} />
      {!docente ? <MensajeEstado tipo="error" mensaje={error} onReintentar={cargar} /> : null}

      {docente ? (
        <section className="tarjeta">
          <h2>Editar perfil de docente</h2>
          <form onSubmit={guardar}>
            <CampoFormulario
              id="codigo-docente"
              etiqueta="Código de docente"
              value={formulario.codigoDocente}
              onChange={(evento) => cambiarCampo('codigoDocente', evento.target.value)}
            />
            <CampoFormulario
              id="dedicacion"
              etiqueta="Dedicación"
              value={formulario.dedicacion}
              onChange={(evento) => cambiarCampo('dedicacion', evento.target.value)}
            />
            <MensajeEstado tipo="error" mensaje={error} />
            <div className="formulario__acciones">
              <Boton type="submit" cargando={guardando}>Guardar cambios</Boton>
              <Boton variante="secundario" onClick={() => setDocente(null)} disabled={guardando}>
                Cancelar
              </Boton>
            </div>
          </form>
        </section>
      ) : null}

      <section className="tarjeta">
        <h2>Perfiles registrados</h2>
        <Tabla
          columnas={columnas}
          filas={docentes}
          cargando={cargando}
          error={null}
          vacio="No se encontraron perfiles de docente."
        />
      </section>
    </>
  );
}
