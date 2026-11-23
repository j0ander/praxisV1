import { defineConfig } from 'vite';

const backend = process.env.PRAXIS_API || 'http://127.0.0.1:8080';

const rutasApi = [
  '/auth',
  '/usuarios',
  '/estudiantes',
  '/docentes',
  '/facultades',
  '/carreras',
  '/periodos',
  '/tiposPractica',
  '/empresas',
  '/convenios',
];

function proxyApi() {
  return Object.fromEntries(rutasApi.map((ruta) => [ruta, { target: backend, changeOrigin: false }]));
}

export default defineConfig({
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: proxyApi(),
  },
  preview: {
    host: '127.0.0.1',
    port: 4173,
    proxy: proxyApi(),
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
