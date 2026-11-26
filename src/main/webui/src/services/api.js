const MENSAJES_ESTADO = {
  400: 'Los datos enviados no son válidos.',
  401: 'Correo o contraseña incorrectos.',
  404: 'No se encontró el registro solicitado.',
  409: 'La operación no puede completarse por una regla de negocio.',
  500: 'Ocurrió un error en el servidor.',
};


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
    401: 'CREDENCIALES_INVALIDAS',
    404: 'NO_ENCONTRADO',
    409: 'REGLA_NEGOCIO',
    500: 'ERROR_INTERNO',
  }[estado];
}

export async function solicitar(ruta, { metodo = 'GET', cuerpo, parametros } = {}) {
  const opciones = {
    method: metodo,
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
