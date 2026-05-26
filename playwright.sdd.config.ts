import { defineConfig, devices } from '@playwright/test';

/**
 * Config dedicada para los `.spec.ts` cacheados por el skill `/sdd-debug-app`.
 *
 * Los specs que ese skill genera al pasar un test viven en
 *   `.sdd/drafts/<iniciativa>/implementation/test_e2e/T-NNN_<kebab>.spec.ts`
 * y la config base (`playwright.config.ts`) NO los descubre porque su
 * `testDir` es `./tests`. Esta config amplía la búsqueda a `.sdd/` para
 * poder ejecutarlos con:
 *
 *   npx playwright test --config=playwright.sdd.config.ts <ruta-del-spec> --project=chromium
 *
 * Requisito: la app debe estar levantada en http://localhost:8080 (no hay
 * `webServer`; el skill la arranca con ./run.sh antes de ejecutar).
 */
export default defineConfig({
  testDir: './.sdd',
  testMatch: '**/implementation/test_e2e/*.spec.ts',
  /* Un test por fichero, ejecutados de uno en uno: comparten la app y su estado. */
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  /* `list` no levanta el servidor del reporter HTML, que colgaría al runner desatendido. */
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});