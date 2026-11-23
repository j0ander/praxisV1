export const RUC_VALIDA = /^\d{13}$/;

export function validarDatosEmpresa(formulario) {
  const errores = {};
  if (!RUC_VALIDA.test((formulario.ruc || '').trim())) {
    errores.ruc = 'El RUC debe tener 13 dígitos.';
  }
  if (!(formulario.razonSocial || '').trim()) {
    errores.razonSocial = 'La razón social es obligatoria.';
  }
  return errores;
}
