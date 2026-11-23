const EVENTO_SESION_EXPIRADA = 'praxis:sesion-expirada';

const MENSAJES_ESTADO = {
  400: 'Los datos enviados no son válidos.',
  401: 'Su sesión ha vencido. Ingrese nuevamente.',
  403: 'No tiene permiso para realizar esta operación.',
  404: 'No se encontró el registro solicitado.',
  409: 'La operación no puede completarse por una regla de negocio.',
  500: 'Ocurrió un error en el servidor.',
};

// `codigo` solo contiene códigos de CONTRATOS_REST.md; `codigoInterno` clasifica fallos del cliente.
export class ApiError extends Error {
  constructor(estado, mensaje, codigo, codigoInterno = null) {
    super(mensaje);
    this.name = 'ApiError';
    this.estado = estado;
    this.codigo = codigo ?? null;
    this.codigoInterno = codigoInterno;
  }
}

function construirUrl(ruta, parametros) {
  const url = new URL(ruta, window.location.origin);
  for (const [nombre, valor] of Object.entries(parametros || {})) {
    if (valor !== null && valor !== undefined && valor !== '') {
      url.searchParams.set(nombre, String(valor));
    }
  }
  return url.toString();
}

function codigoPorEstado(estado) {
  return {
    400: 'DATOS_INVALIDOS',
    401: 'SIN_SESION',
    403: 'SIN_PERMISO',
    404: 'NO_ENCONTRADO',
    409: 'REGLA_NEGOCIO',
    500: 'ERROR_INTERNO',
  }[estado];
}

export async function solicitar(ruta, { metodo = 'GET', cuerpo, parametros } = {}) {
  const opciones = {
    method: metodo,
    credentials: 'include',
    headers: { Accept: 'application/json' },
  };

  if (cuerpo !== undefined) {
    opciones.headers['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(construirUrl(ruta, parametros), opciones);
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor.', null, 'ERROR_CONEXION');
  }

  if (respuesta.status === 204) {
    return null;
  }

  const texto = await respuesta.text();
  let datos = null;
  if (texto.trim()) {
    try {
      datos = JSON.parse(texto);
    } catch {
      throw new ApiError(respuesta.status, 'El servidor devolvió una respuesta inesperada.', null, 'RESPUESTA_INVALIDA');
    }
  }

  if (!respuesta.ok) {
    const mensaje = datos?.mensaje || MENSAJES_ESTADO[respuesta.status] || 'La operación no pudo completarse.';
    const codigo = datos?.codigo || codigoPorEstado(respuesta.status);
    if (respuesta.status === 401 && ruta !== '/auth/login') {
      window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA));
    }
    throw new ApiError(respuesta.status, mensaje, codigo);
  }

  return datos;
}

export function mensajeDeError(error) {
  if (error instanceof ApiError) {
    return error.message;
  }
  return 'Ocurrió un error inesperado.';
}
