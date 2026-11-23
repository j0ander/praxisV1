import { useEffect, useState } from 'react';

export function navegar(ruta, { reemplazar = false } = {}) {
  if (reemplazar) {
    window.history.replaceState({}, '', ruta);
  } else {
    window.history.pushState({}, '', ruta);
  }
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export function usarRuta() {
  const [ruta, setRuta] = useState(() => window.location.pathname);

  useEffect(() => {
    const actualizar = () => setRuta(window.location.pathname);
    window.addEventListener('popstate', actualizar);
    return () => window.removeEventListener('popstate', actualizar);
  }, []);

  return ruta;
}
