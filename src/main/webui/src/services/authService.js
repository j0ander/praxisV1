import { solicitar } from './api';

export function iniciarSesion(correo, password) {
  return solicitar('/auth/login', { metodo: 'POST', cuerpo: { correo, password } });
}

export function sesionActual() {
  return solicitar('/auth/yo');
}

export function cerrarSesion() {
  return solicitar('/auth/logout', { metodo: 'POST' });
}
