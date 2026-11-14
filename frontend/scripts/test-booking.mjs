import { build } from 'esbuild';
import { mkdir } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
await mkdir('.angular/booking-tests', { recursive: true });
await build({entryPoints:['src/app/features/patient/booking.component.test.ts'],outfile:'.angular/booking-tests/test.mjs',bundle:true,platform:'node',format:'esm',packages:'external',tsconfig:'tsconfig.json'});
const result=spawnSync(process.execPath,['--test','.angular/booking-tests/test.mjs'],{stdio:'inherit'});
process.exit(result.status ?? 1);
