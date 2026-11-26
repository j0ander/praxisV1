import { defineConfig } from 'vite';

const backend = process.env.PRAXIS_API || 'http://127.0.0.1:8080';

const prefijosApi = [
  'usuarios',
  'estudiantes',
  'docentes',
  'facultades',
  'carreras',
  'periodos',
  'tiposPractica',
  'empresas',
  'convenios',
].join('|');



const rutaApi = `^/(?:${prefijosApi})(?:/|$)`;

function proxyApi() {
  return {
    [rutaApi]: {
      target: backend,
      changeOrigin: false,
      bypass(req) {
        if (String(req.headers.accept || '').includes('text/html')) {
          return '/index.html';
        }
      },
    },
  };
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
