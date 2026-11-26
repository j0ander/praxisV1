import { solicitar } from './api';

export function iniciarSesion(correo, password) {
  return solicitar('/usuarios/login', { metodo: 'POST', cuerpo: { correo, password } });
}
